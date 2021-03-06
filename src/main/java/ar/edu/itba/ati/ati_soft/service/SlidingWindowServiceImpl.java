package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageThresholdService;
import ar.edu.itba.ati.ati_soft.interfaces.SlidingWindowService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.TriFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Concrete implementation of {@link SlidingWindowService}.
 */
@Service
public class SlidingWindowServiceImpl implements SlidingWindowService {

    private final ImageThresholdService imageThresholdService;

    @Autowired
    public SlidingWindowServiceImpl(ImageThresholdService imageThresholdService) {
        this.imageThresholdService = imageThresholdService;
    }

    // ================================================================================================================
    // Filters
    // ================================================================================================================

    @Override
    public Image applyMeanFilter(Image image, int windowLength) {
        return applyFilter(image, windowLength,
                array -> Arrays.stream(array).flatMap(Arrays::stream)
                        .mapToDouble(i -> i)
                        .average()
                        .orElseThrow(RuntimeException::new));
    }

    @Override
    public Image applyMedianFilter(Image image, int windowLength) {
        return applyFilter(image, windowLength,
                array -> {
                    final long arrayAmount = Arrays.stream(array).flatMap(Arrays::stream).count();
                    return Arrays.stream(array).flatMap(Arrays::stream)
                            .mapToDouble(i -> i)
                            .sorted()
                            .skip((arrayAmount - 1) / 2)
                            .limit(2 - arrayAmount % 2)
                            .average()
                            .orElseThrow(RuntimeException::new);
                });
    }

    @Override
    public Image applyWeightMedianFilter(Image image, Integer[][] weights) {
        Assert.notNull(weights, "The weights array must not be null");
        Assert.notEmpty(weights, "The weights array must not be empty");
        final int length = weights.length;
        Assert.isTrue(Arrays.stream(weights)
                .filter(internalArray -> internalArray == null || internalArray.length != length)
                .count() == 0, "The weights array must be square");

        return applyFilter(image, length,
                array -> {
                    final int arrayAmount = Arrays.stream(weights).flatMap(Arrays::stream).mapToInt(i -> i).sum();
                    return IntStream.range(0, array.length)
                            .mapToObj(x -> IntStream.range(0, array[x].length)
                                    .mapToObj(y -> Collections.nCopies(weights[x][y], array[x][y]))
                                    .flatMap(Collection::stream))
                            .flatMap(Function.identity())
                            .mapToDouble(i -> i)
                            .sorted()
                            .skip((arrayAmount - 1) / 2)
                            .limit(2 - arrayAmount % 2)
                            .average()
                            .orElseThrow(RuntimeException::new);
                });
    }

    @Override
    public Image applyGaussianFilter(Image image, double standardDeviation) {
        Assert.isTrue(standardDeviation > 0, "The standard deviation must be positive");
        final int margin = (int) standardDeviation;
        // First calculate values using the Gaussian function
        final double variance = standardDeviation * standardDeviation; // Avoid recalculating this
        final double factor = 1 / (2 * Math.PI * variance); // Avoid recalculating this
        final Double[][] unfinishedMask = IntStream.range(-margin, margin + 1)
                .mapToObj(x -> IntStream.range(-margin, margin + 1)
                        .mapToObj(y -> factor * Math.exp(-(x * x + y * y) / variance))
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);
        // Then, calculate the sum of values
        final double sum = Arrays.stream(unfinishedMask)
                .flatMap(Arrays::stream)
                .reduce(0.0, (o1, o2) -> o1 + o2);
        // Finally, produce the mask by dividing each value with the calculated sum
        final Double[][] mask = Arrays.stream(unfinishedMask)
                .map(internal -> Arrays.stream(internal)
                        .map(value -> value / sum)
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);

        return filterWithMask(image, mask);
    }

    @Override
    public Image applyBilateralFilter(Image image, double spatialStd, double rangeStd, int windowSize) {
        return doApplyBilateralFilter(image, spatialStd, rangeStd, windowSize);
    }

    @Override
    public Image applyBilateralFilterWithCIELabColor(Image image, double spatialStd, double rangeStd, int windowSize) {
        Assert.isTrue(image.getBands() == 1 || image.getBands() == 3,
                "Only one band or three bands images (For three bands images, the image must be RGB)");
        // If the image is gray (i.e has only one band), then apply the filter as is)
        // Else, an RGB -> XYZ -> CIE-Lab -> Bilateral Filter -> XYZ -> RGB transformation must be done
        if (image.getBands() == 1) {
            return applyBilateralFilterForGrayImage(image, spatialStd, rangeStd, windowSize);
        }
        return applyBilateralFilterForColorImage(image, spatialStd, rangeStd, windowSize);
    }


    // ================================================================================================================
    // Border detection
    // ================================================================================================================

    @Override
    public Image applyHighPassFilter(Image image, int windowLength) {
        Assert.isTrue(windowLength > 0, "The window length must be positive");
        Assert.isTrue(windowLength % 2 == 1, "The window length must not be even");
        final int size = windowLength * windowLength;
        final Double[][] mask = IntStream.range(0, windowLength)
                .mapToObj(x -> IntStream.range(0, windowLength)
                        .mapToObj(y -> -1.0 / size)
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);
        final int center = windowLength / 2;
        mask[center][center] *= 1 - size;

        return filterWithMask(ImageManipulationHelper.toGray(image), mask);
    }


    @Override
    public Image prewittGradientOperatorBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithModulus(ImageManipulationHelper.toGray(image), PrewittMask.TOP, PrewittMask.RIGHT);
    }

    @Override
    public Image sobelGradientOperatorBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithModulus(ImageManipulationHelper.toGray(image), SobelMask.TOP, SobelMask.RIGHT);
    }

    @Override
    public Image anonymousMaxDirectionBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithMax(ImageManipulationHelper.toGray(image), AnonymousMask.values());
    }

    @Override
    public Image kirshMaxDirectionBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithMax(ImageManipulationHelper.toGray(image), KirshMask.values());
    }

    @Override
    public Image prewittMaxDirectionBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithMax(ImageManipulationHelper.toGray(image), PrewittMask.values());
    }

    @Override
    public Image sobelMaxDirectionBorderDetectionMethod(Image image) {
        return multiMaskFilteringWithMax(ImageManipulationHelper.toGray(image), SobelMask.values());
    }

    @Override
    public Image laplaceMethod(Image image) {
        return laplaceMethod(image, (prev, actual) -> true);
    }

    @Override
    public Image laplaceMethodWithSlopeEvaluation(Image image, double slopeThreshold) {
        return laplaceMethod(image, (prev, actual) -> Math.abs(prev - actual) >= slopeThreshold);
    }

    @Override
    public Image laplaceOfGaussianMethod(Image image, double sigma) {
        return laplaceOfGaussianMethod(image, sigma, (prev, actual) -> true);
    }

    @Override
    public Image laplaceOfGaussianWithSlopeEvaluation(Image image, double sigma, double slopeThreshold) {
        return laplaceOfGaussianMethod(image, sigma, (prev, actual) -> Math.abs(prev - actual) >= slopeThreshold);
    }

    @Override
    public Image suppressNoMaxPixels(Image image, double sigma) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bands = image.getBands();
        final Image grayImage = ImageManipulationHelper.toGray(image);
        final Image filtered = sigma == 0 ? grayImage : applyGaussianFilter(grayImage, sigma);
        final Image gx = filterWithMask(filtered, SobelMask.TOP.getMask());
        final Image gy = filterWithMask(filtered, SobelMask.RIGHT.getMask());
        // Use the modulus instead of the 1st-norm, as is has better results
        final Image gradientImage = ImageManipulationHelper.createApplying(() -> Image.empty(width, height, bands),
                ((x, y, b) -> {
                    final double xGradient = gx.getSample(x, y, b);
                    final double yGradient = gy.getSample(x, y, b);
                    return Math.sqrt(xGradient * xGradient + yGradient * yGradient);
                }));

        final Image anglesImage = ImageManipulationHelper.createApplying(() -> Image.empty(width, height, bands),
                new AnglesFunction(gx, gy).andThen(SlidingWindowServiceImpl::correctAngle));

        return ImageManipulationHelper.createApplying(gradientImage,
                (x, y, b, v) -> {
                    if (v <= 0) {
                        return 0d;
                    }
                    final Direction direction = Direction.fromAngle(anglesImage.getSample(x, y, b));

                    final int prevRow = x - direction.getX();
                    final int prevColumn = y - direction.getY();
                    final int nextRow = x + direction.getX();
                    final int nextColumn = y + direction.getY();
                    // Check index ranges first, and then adjacent pixels along the direction
                    if (prevRow < 0 || prevRow >= width || prevColumn < 0 || prevColumn >= height
                            || nextRow < 0 || nextRow >= width || nextColumn < 0 || nextColumn >= height
                            || gradientImage.getSample(prevRow, prevColumn, b) > v
                            || gradientImage.getSample(nextRow, nextColumn, b) > v) {
                        return 0d;
                    }
                    return v;
                });
    }

    @Override
    public Image cannyDetection(Image image, double sigma) {
        return imageThresholdService.hysteresisThreshold(suppressNoMaxPixels(image, sigma));
    }

    @Override
    public Image susanDetection(Image image, double t) {
        // Avoid recalculating these values...
        final long amountOfPixelsInMask = Arrays.stream(SUSAN_CIRCULAR_MASK).flatMap(Arrays::stream)
                .filter(v -> v == 1d)
                .count();
        final int centerRow = SUSAN_CIRCULAR_MASK.length / 2;
        final int centerColumn = SUSAN_CIRCULAR_MASK.length / 2;

        final Image circularImage = applyFilter(ImageManipulationHelper.toGray(image), SUSAN_CIRCULAR_MASK.length,
                window -> {
                    final double center = window[centerRow][centerColumn];
                    final double nr0 = IntStream.range(0, SUSAN_CIRCULAR_MASK.length)
                            .mapToObj(x -> IntStream.range(0, SUSAN_CIRCULAR_MASK.length)
                                    .filter(y -> !(x == centerRow && y == centerColumn))
                                    .filter(y -> SUSAN_CIRCULAR_MASK[x][y] == 1d)
                                    .mapToObj(y -> window[x][y]))
                            .flatMap(Function.identity())
                            .map(r -> Math.abs(r - center) < t ? 1d : 0d)
                            .reduce(0d, (o1, o2) -> o1 + o2);
                    return 1 - (nr0 / amountOfPixelsInMask);
                });
        final int width = image.getWidth();
        final int height = image.getHeight();

        return ImageManipulationHelper.createApplying(() -> Image.empty(width, height, 3),
                (x, y) -> {
                    // The image is gray so only one sample is needed
                    final double v = circularImage.getSample(x, y, 0);
                    if (v >= 0.65 && v <= 0.85) {
                        // In this case, it is a corner
                        return new Double[]{255d, 0d, 0d};
                    } else if (v >= 0.4 && v <= 0.6) {
                        // In this case, it is a corner
                        return new Double[]{0d, 255d, 0d};
                    }
                    return new Double[]{0d, 0d, 0d};
                });
    }

    // ================================================================================================================
    // Masks
    // ================================================================================================================

    private final static Double[][] ANONYMOUS_MASK = {{1d, 1d, 1d}, {1d, -2d, 1d}, {-1d, -1d, -1d}};
    private final static Double[][] KIRSH_MASK = {{5d, 5d, 5d}, {-3d, 0d, -3d}, {-3d, -3d, -3d}};
    private final static Double[][] PREWITT_MASK = {{1d, 1d, 1d}, {0d, 0d, 0d}, {-1d, -1d, -1d}};
    private final static Double[][] SOBEL_MASK = {{1d, 2d, 1d}, {0d, 0d, 0d}, {-1d, -2d, -1d}};
    private final static Double[][] LAPLACE_MASK = {{0d, -1d, 0d}, {-1d, 4d, -1d}, {0d, -1d, 0d}};
    private final static Double[][] SUSAN_CIRCULAR_MASK = {
            {0d, 0d, 1d, 1d, 1d, 0d, 0d},
            {0d, 1d, 1d, 1d, 1d, 1d, 0d},
            {1d, 1d, 1d, 1d, 1d, 1d, 1d},
            {1d, 1d, 1d, 1d, 1d, 1d, 1d},
            {1d, 1d, 1d, 1d, 1d, 1d, 1d},
            {0d, 1d, 1d, 1d, 1d, 1d, 0d},
            {0d, 0d, 1d, 1d, 1d, 0d, 0d},
    };

    /**
     * Enum containing the anonymous mask in all directions.
     */
    private enum AnonymousMask implements MaskHelper.MaskContainer {
        TOP(ANONYMOUS_MASK),
        TOP_LEFT(MaskHelper.rotate3x3Mask(TOP, 1)),
        LEFT(MaskHelper.rotate3x3Mask(TOP, 2)),
        BOTTOM_LEFT(MaskHelper.rotate3x3Mask(TOP, 3)),
        BOTTOM(MaskHelper.mirrorMask(TOP)),
        BOTTOM_RIGHT(MaskHelper.mirrorMask(TOP_LEFT)),
        RIGHT(MaskHelper.mirrorMask(LEFT)),
        TOP_RIGHT(MaskHelper.mirrorMask(BOTTOM_LEFT));

        /**
         * The mask contained by each value.
         */
        private final Double[][] mask;

        /**
         * Constructor.
         *
         * @param mask The mask contained by each value.
         * @throws IllegalArgumentException If the given {@code mask} is invalid.
         */
        AnonymousMask(Double[][] mask) throws IllegalArgumentException {
            // First, validate the mask.
            MaskHelper.validateMask(mask); // Makes sure that the mask is not null or empty, and is square.
            Assert.isTrue(mask.length == 3, "The mask must be a 3x3 square matrix");
            // Then, set the mask.
            this.mask = mask;
        }

        @Override
        public Double[][] getMask() {
            return mask;
        }
    }

    /**
     * Enum containing the Kirsh's mask in all directions.
     */
    private enum KirshMask implements MaskHelper.MaskContainer {
        TOP(KIRSH_MASK),
        TOP_LEFT(MaskHelper.rotate3x3Mask(TOP, 1)),
        LEFT(MaskHelper.rotate3x3Mask(TOP, 2)),
        BOTTOM_LEFT(MaskHelper.rotate3x3Mask(TOP, 3)),
        BOTTOM(MaskHelper.mirrorMask(TOP)),
        BOTTOM_RIGHT(MaskHelper.mirrorMask(TOP_LEFT)),
        RIGHT(MaskHelper.mirrorMask(LEFT)),
        TOP_RIGHT(MaskHelper.mirrorMask(BOTTOM_LEFT));

        /**
         * The mask contained by each value.
         */
        private final Double[][] mask;

        /**
         * Constructor.
         *
         * @param mask The mask contained by each value.
         * @throws IllegalArgumentException If the given {@code mask} is invalid.
         */
        KirshMask(Double[][] mask) throws IllegalArgumentException {
            // First, validate the mask.
            MaskHelper.validateMask(mask); // Makes sure that the mask is not null or empty, and is square.
            Assert.isTrue(mask.length == 3, "The mask must be a 3x3 square matrix");
            // Then, set the mask.
            this.mask = mask;
        }

        @Override
        public Double[][] getMask() {
            return mask;
        }
    }

    /**
     * Enum containing the Prewitt's mask in all directions.
     */
    private enum PrewittMask implements MaskHelper.MaskContainer {
        TOP(PREWITT_MASK),
        TOP_LEFT(MaskHelper.rotate3x3Mask(TOP, 1)),
        LEFT(MaskHelper.rotate3x3Mask(TOP, 2)),
        BOTTOM_LEFT(MaskHelper.rotate3x3Mask(TOP, 3)),
        BOTTOM(MaskHelper.mirrorMask(TOP)),
        BOTTOM_RIGHT(MaskHelper.mirrorMask(TOP_LEFT)),
        RIGHT(MaskHelper.mirrorMask(LEFT)),
        TOP_RIGHT(MaskHelper.mirrorMask(BOTTOM_LEFT));

        /**
         * The mask contained by each value.
         */
        private final Double[][] mask;

        /**
         * Constructor.
         *
         * @param mask The mask contained by each value.
         * @throws IllegalArgumentException If the given {@code mask} is invalid.
         */
        PrewittMask(Double[][] mask) throws IllegalArgumentException {
            // First, validate the mask.
            MaskHelper.validateMask(mask); // Makes sure that the mask is not null or empty, and is square.
            Assert.isTrue(mask.length == 3, "The mask must be a 3x3 square matrix");
            // Then, set the mask.
            this.mask = mask;
        }

        @Override
        public Double[][] getMask() {
            return mask;
        }
    }

    /**
     * Enum containing the Sobel's mask in all directions.
     */
    private enum SobelMask implements MaskHelper.MaskContainer {
        TOP(SOBEL_MASK),
        TOP_LEFT(MaskHelper.rotate3x3Mask(TOP, 1)),
        LEFT(MaskHelper.rotate3x3Mask(TOP, 2)),
        BOTTOM_LEFT(MaskHelper.rotate3x3Mask(TOP, 3)),
        BOTTOM(MaskHelper.mirrorMask(TOP)),
        BOTTOM_RIGHT(MaskHelper.mirrorMask(TOP_LEFT)),
        RIGHT(MaskHelper.mirrorMask(LEFT)),
        TOP_RIGHT(MaskHelper.mirrorMask(BOTTOM_LEFT));

        /**
         * The mask contained by each value.
         */
        private final Double[][] mask;

        /**
         * Constructor.
         *
         * @param mask The mask contained by each value.
         * @throws IllegalArgumentException If the given {@code mask} is invalid.
         */
        SobelMask(Double[][] mask) throws IllegalArgumentException {
            // First, validate the mask.
            MaskHelper.validateMask(mask); // Makes sure that the mask is not null or empty, and is square.
            Assert.isTrue(mask.length == 3, "The mask must be a 3x3 square matrix");
            // Then, set the mask.
            this.mask = mask;
        }

        @Override
        public Double[][] getMask() {
            return mask;
        }
    }


    // ================================================================================================================
    // Helper methods
    // ================================================================================================================

    /**
     * Performs bilateral filtering for gray images.
     *
     * @param image      The {@link Image} to which the filter will be applied.
     * @param spatialStd The Gaussian standard deviation for the spatial domain filtering.
     * @param rangeStd   The Gaussian standard deviation for the range filtering.
     * @param windowSize The size of the window to be used.
     * @return a new {@link Image} with the filter applied.
     */
    private static Image applyBilateralFilterForGrayImage(Image image,
                                                          double spatialStd, double rangeStd, int windowSize) {
        Assert.isTrue(image.getBands() == 1, "Only gray images");
        return doApplyBilateralFilter(image, spatialStd, rangeStd, windowSize);
    }

    /**
     * Performs bilateral filtering for color images, applying an RGB to CIE-Lab transformation before filtering,
     * and reconverting to RGB after the process.
     *
     * @param image      The {@link Image} to which the filter will be applied.
     * @param spatialStd The Gaussian standard deviation for the spatial domain filtering.
     * @param rangeStd   The Gaussian standard deviation for the range filtering.
     * @param windowSize The size of the window to be used.
     * @return a new {@link Image} with the filter applied.
     */
    private static Image applyBilateralFilterForColorImage(Image image,
                                                           double spatialStd, double rangeStd, int windowSize) {
        Assert.isTrue(image.getBands() == 3, "Only three bands images (in RGB color space)");
        final Image cieLabImage = ColorHelper.rgbToCieLab(ImageManipulationHelper.normalize(image));
        final Image filteredCieLab = doApplyBilateralFilter(cieLabImage, spatialStd, rangeStd, windowSize);
        return ColorHelper.cieLabToRGB(filteredCieLab);
    }


    /**
     * Performs the bilateral filtering of the given {@code image}.
     *
     * @param image      The {@link Image} to which the filter will be applied.
     * @param spatialStd The Gaussian standard deviation for the spatial domain filtering.
     * @param rangeStd   The Gaussian standard deviation for the range filtering.
     * @param windowSize The size of the window to be used.
     * @return a new {@link Image} with the filter applied.
     * @apiNote This method assumes that the images are 1-band or 3-band images (being CIE-Lab in this case).
     */
    private static Image doApplyBilateralFilter(Image image, double spatialStd, double rangeStd, int windowSize) {
        Assert.isTrue(spatialStd > 0 && rangeStd > 0, "Both standard deviations must be positive");
        final int margin = windowSize / 2;
        final double spatialFactor = 2 * spatialStd * spatialStd;
        final Double[][] spatialGauss = IntStream.range(-margin, margin + 1)
                .mapToObj(x -> IntStream.range(-margin, margin + 1)
                        .mapToObj(y -> Math.exp(-(x * x + y * y) / spatialFactor))
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);
        final double rangeFactor = 2 * rangeStd * rangeStd;
        // The bilateral filtering is composed of a spatial domain filter (which only depends on the distance of pixels)
        // and a range filter (which depends on the intensity value of the image in a given region)
        // That's why we must use the applyFilter method, which receives a function that depends on a portion
        // of an image (the so called window). This method allows to operate with the window,
        // defining the mask based on it.
        return applyFilter(image, windowSize,
                window -> {
                    // First get the center of the window, which will be used for range filtering
                    final double center = window[margin][margin];
                    // Then calculate the unfinished mask, which lacks the division of the sum
                    final Double[][] partiallyFiltered = IntStream.range(0, window.length)
                            .mapToObj(x -> IntStream.range(0, window[x].length)
                                    .mapToObj(y -> {
                                        final double windowValue = window[x][y];
                                        final double difference = windowValue - center;
                                        final double distance = difference * difference;
                                        final double exponential = Math.exp(-distance / rangeFactor);
                                        return exponential * spatialGauss[x][y]; // Combination of both filters
                                    })
                                    .toArray(Double[]::new)
                            ).toArray(Double[][]::new);
                    // Calculate the sum of the mask
                    final double sum = Arrays.stream(partiallyFiltered)
                            .flatMap(Arrays::stream)
                            .reduce(0.0, (o1, o2) -> o1 + o2);
                    // Then, apply the filtering with the mask by multiplying each element of the window
                    // with its corresponding element in the mask, and them summing all the resultant elements
                    // The result is then divided by the sum (acts as a normalization)
                    return IntStream.range(0, window.length)
                            .mapToObj(x -> IntStream.range(0, window[x].length)
                                    .mapToObj(y -> partiallyFiltered[x][y] * window[x][y]))
                            .flatMap(Function.identity())
                            .reduce(0.0, (o1, o2) -> o1 + o2) / sum;
                });
    }


    /**
     * Performs a multi-mask filtering, according to the given
     * {@link ar.edu.itba.ati.ati_soft.service.MaskHelper.MaskContainer}s,
     * applying the modulus between all the generated border images.
     *
     * @param image          The {@link Image} to be filtered.
     * @param maskContainers The {@link ar.edu.itba.ati.ati_soft.service.MaskHelper.MaskContainer}
     *                       that holds the masks to be applied.
     * @return The filtered image.
     */
    private static Image multiMaskFilteringWithModulus(Image image, MaskHelper.MaskContainer... maskContainers) {
        return Arrays.stream(maskContainers)
                .parallel()
                .map(MaskHelper.MaskContainer::getMask)
                .map(mask -> filterWithMask(image, mask))
                .map(img -> ImageManipulationHelper.createApplying(img, (x, y, b, v) -> v * v))
                .reduce(((img1, img2) ->
                        ImageManipulationHelper.createApplying(img1, (x, y, b, v) -> v + img2.getSample(x, y, b))))
                .map(img -> ImageManipulationHelper.createApplying(img, (x, y, b, v) -> Math.sqrt(v)))
                .orElseThrow(() -> new RuntimeException("This should not happen"));
    }

    /**
     * Performs a multi-mask filtering, according to the given
     * {@link ar.edu.itba.ati.ati_soft.service.MaskHelper.MaskContainer}s,
     * applying the max between all the generated border images.
     *
     * @param image          The {@link Image} to be filtered.
     * @param maskContainers The {@link ar.edu.itba.ati.ati_soft.service.MaskHelper.MaskContainer}
     *                       that holds the masks to be applied.
     * @return The filtered image.
     */
    private static Image multiMaskFilteringWithMax(Image image, MaskHelper.MaskContainer... maskContainers) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bands = image.getBands();
        return Arrays.stream(maskContainers)
                .parallel()
                .map(MaskHelper.MaskContainer::getMask)
                .map(mask -> filterWithMask(image, mask))
                .map(img -> ImageManipulationHelper.createApplying(img, (x, y, b, v) -> Math.abs(v)))
                .reduce(Image.empty(width, height, bands),
                        (img1, img2) -> ImageManipulationHelper.createApplying(img1,
                                (x, y, b, v) -> Math.max(v, img2.getSample(x, y, b))));
    }

    /**
     * Applies the Laplace method for border detection.
     *
     * @param image       The {@link Image} to which the method will be applied.
     * @param acceptSlope A {@link BiFunction} that takes to contiguous pixels, calculates the slope,
     *                    and tells whether this slope is acceptable (i.e can be considered a border).
     * @return The processed {@link Image}.
     */
    private static Image laplaceMethod(Image image, BiFunction<Double, Double, Boolean> acceptSlope) {
        return secondDerivativeMethod(image, LAPLACE_MASK, acceptSlope);
    }

    /**
     * Applies the Laplace method for border detection.
     *
     * @param image       The {@link Image} to which the method will be applied.
     * @param acceptSlope A {@link BiFunction} that takes to contiguous pixels, calculates the slope,
     *                    and tells whether this slope is acceptable (i.e can be considered a border).
     * @return The processed {@link Image}.
     */
    private static Image laplaceOfGaussianMethod(Image image, double sigma,
                                                 BiFunction<Double, Double, Boolean> acceptSlope) {
        Assert.isTrue(sigma > 0, "The standard deviation must be positive");
        final int margin = (int) (sigma * 3);
        final double variance = sigma * sigma; // Avoid recalculating this
        final double factor = -1 / (Math.sqrt(2 * Math.PI) * variance * sigma); // Avoid recalculating this
        final Double[][] mask = IntStream.range(-margin, margin + 1)
                .mapToObj(x -> IntStream.range(-margin, margin + 1)
                        .mapToObj(y -> (x * x + y * y) / variance)
                        .map(value -> factor * (2 - value) * Math.exp(-value / 2))
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);
        return secondDerivativeMethod(image, mask, acceptSlope);
    }

    /**
     * Applies the a second derivative method for border detection.
     *
     * @param image       The {@link Image} to which the method will be applied.
     * @param mask        The mask to be applied.
     * @param acceptSlope A {@link BiFunction} that takes to contiguous pixels, calculates the slope,
     *                    and tells whether this slope is acceptable (i.e can be considered a border).
     * @return The processed {@link Image}.
     */
    private static Image secondDerivativeMethod(Image image, Double[][] mask,
                                                BiFunction<Double, Double, Boolean> acceptSlope) {
        final Image maskImage = filterWithMask(ImageManipulationHelper.toGray(image), mask);
        final Supplier<Image> emptyImageSupplier =
                () -> Image.empty(maskImage.getWidth(), maskImage.getHeight(), maskImage.getBands());

        final Image zeroCrossByColumn = ImageManipulationHelper.createApplying(emptyImageSupplier,
                (x, y, b) -> borderPixel(0, maskImage.getWidth() - 1, x,
                        position -> maskImage.getSample(position, y, b), acceptSlope));
        final Image zeroCrossByRow = ImageManipulationHelper.createApplying(emptyImageSupplier,
                (x, y, b) -> borderPixel(0, maskImage.getHeight() - 1, y,
                        position -> maskImage.getSample(x, position, b), acceptSlope));

        return ImageManipulationHelper.createApplying(emptyImageSupplier,
                (x, y, b) -> zeroCrossByColumn.getSample(x, y, b) + zeroCrossByRow.getSample(x, y, b) == 0 ? 0d : 255d);
    }

    /**
     * Returns a border pixel (i.e {@code 0.0} if there is no border, or {@code 255.0} if there is border).
     * This method uses the zero cross technique.
     *
     * @param lowerLimit    The lower limit to iterate.
     * @param upperLimit    The upper limit to iterate.
     * @param position      The actual position (i.e
     * @param pixelSupplier The function that returns a pixel according to the actual position.
     * @return The border pixel.
     */
    private static double borderPixel(int lowerLimit, int upperLimit, int position,
                                      Function<Integer, Double> pixelSupplier,
                                      BiFunction<Double, Double, Boolean> acceptSlope) {
        Assert.isTrue(position >= lowerLimit && position <= upperLimit, "The position is out of range");

        if (position == lowerLimit) {
            return 0d;
        }
        final double pixel = pixelSupplier.apply(position);
        final double prev = pixelSupplier.apply(position - 1);
        if (position == upperLimit) {
            return changeOfSign(prev, pixel) && acceptSlope.apply(prev, pixel) ? 255d : 0;
        }
        final double next = pixelSupplier.apply(position + 1);
        return changeOfSign(prev, pixel, next) && acceptSlope.apply(prev, pixel) ? 255d : 0;
    }

    /**
     * Checks if there is a change of sign between the three pixels.
     *
     * @param prev  The previous pixel.
     * @param pixel The actual pixel.
     * @param next  The next pixel.
     * @return {@code true} if there is change of sign, or {@code false} otherwise.
     */
    private static boolean changeOfSign(double prev, double pixel, double next) {
        if (pixel == 0d) {
            return changeOfSign(prev, next);
        }
        return changeOfSign(prev, pixel);
    }

    /**
     * Checks if there is a change of sign between the two pixels.
     *
     * @param prev  The previous pixel.
     * @param pixel The actual pixel.
     * @return {@code true} if there is change of sign, or {@code false} otherwise.
     */
    private static boolean changeOfSign(double prev, double pixel) {
        return prev * pixel < 0;
    }

    /**
     * Corrects the given angle in order to match with the allowed directions for the Canny method.
     *
     * @param rawAngle The raw angle which must be corrected, in radians.
     * @return The corrected angle.
     */
    private static double correctAngle(double rawAngle) {
        final double semiCircleAngle = (rawAngle + Math.PI) % Math.PI;
        return ((int) ((semiCircleAngle + (Math.PI / 8)) / (Math.PI / 4)) % 4) * (Math.PI / 4);
    }


    /**
     * Applies a filter to the given {@link Image}, using the given {@code mask}.
     *
     * @param image The {@link Image} to be filtered.
     * @param mask  The mask to be applied.
     * @return The filtered {@link Image}.
     */
    private static Image filterWithMask(Image image, Double[][] mask) {
        MaskHelper.validateMask(mask);
        return applyFilter(image, mask.length,
                array -> IntStream.range(0, array.length)
                        .mapToObj(x -> IntStream.range(0, array[x].length)
                                .mapToObj(y -> array[x][y] * mask[x][y]))
                        .flatMap(Function.identity())
                        .reduce(0.0, (o1, o2) -> o1 + o2));
    }

    /**
     * Applies a filter to the given {@link Image}, using a mask with the given {@code windowLength},
     * applying the given {@code filterFunction} to calculate the new pixel values.
     * Note that the {@code filterFunction} takes as an argument a window (represented as a two-dimensional array,
     * which holds, for a given channel, the samples of an {@link Image} in a given position),
     * and produces the value for the pixel (i.e the filtered value).
     *
     * @param image          The {@link Image} to which the filtering will be applied.
     * @param windowLength   The window length (i.e the length of the kernel).
     * @param filterFunction A function that takes a two-dimensional array
     *                       that represents sub-raster of the given {@code image}, in a given channel,
     *                       and produces the new value (i.e the filtered value).
     * @return A new {@link Image} instance with the filter applied.
     */
    private static Image applyFilter(Image image, int windowLength, Function<Double[][], Double> filterFunction) {
        if (windowLength < 0) {
            throw new IllegalArgumentException("The length must be positive");
        }
        if (windowLength % 2 == 0) {
            throw new IllegalArgumentException("The window length must not be even");
        }

        final int margin = windowLength / 2;
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bands = image.getBands();
        final Image newImage = Image.homogeneous(width, height, bands, 0d);
        final Double[][] window = new Double[windowLength][windowLength];
        for (int x = margin; x < width - margin; x++) {
            for (int y = margin; y < height - margin; y++) {
                for (int b = 0; b < bands; b++) {
                    fill(x - margin, y - margin, b, image, window);
                    final double filtered = filterFunction.apply(window);
                    newImage.setSample(x, y, b, filtered);
                }
            }
        }
        return newImage;
    }

    /**
     * Fills the given {@code window} with data from the given {@link Image}.
     * This method avoid instantiating a window for each step, saving a lot of memory.
     *
     * @param xInitial Initial 'x' from where data will be taken.
     * @param yInitial Initial 'y' from where data will be taken.
     * @param band     Band from where data will be taken.
     * @param image    The {@link Image} from where data will be taken.
     * @param window   The {@code {@link Double[][]}} instance where data will be outputted.
     * @implNote This method assumes that all params are well formed, in order to avoid extra computing.
     */
    private static void fill(int xInitial, int yInitial, int band, Image image, Double[][] window) {
        for (int x = 0; x < window.length; x++) {
            for (int y = 0; y < window[0].length; y++) {
                window[x][y] = image.getSample(xInitial + x, yInitial + y, band);
            }
        }
    }


    // ================================================================================================================
    // Helper classes and enums
    // ================================================================================================================

    /**
     * A specialization of {@link TriFunction} which, given an 'x', an 'y' and and a band,
     * together with the 'x' and 'y' gradient images, it calculates the angle of them.
     */
    private static final class AnglesFunction implements TriFunction<Integer, Integer, Integer, Double> {
        /**
         * The 'x' gradient.
         */
        private final Image gx;
        /**
         * The 'y' gradient.
         */
        private final Image gy;

        /**
         * Constructor.
         *
         * @param gx The 'x' gradient.
         * @param gy The 'y' gradient.
         */
        private AnglesFunction(Image gx, Image gy) {
            this.gx = gx;
            this.gy = gy;
        }


        @Override
        public Double apply(Integer x, Integer y, Integer b) {
            return Math.atan2(gy.getSample(x, y, b), gx.getSample(x, y, b));
        }
    }

    /**
     * Enum containing the directions for the gradient (i.e to be used by the Canny method).
     */
    private enum Direction {
        /**
         * Horizontal direction (i.e 0º).
         */
        HORIZONTAL(1, 0),
        /**
         * Vertical direction (i.e 90º).
         */
        VERTICAL(0, 1),
        /**
         * Diagonal direction going from the top-right corner to the lower-left (i.e 45º).
         */
        TOP_RIGHT(1, 1),
        /**
         * Diagonal direction going from the top-left corner to the lower-right (i.e 135º).
         */
        TOP_LEFT(-1, 1);

        /**
         * Represents the step in the 'x' axis.
         */
        private final int x;
        /**
         * Represents the step in the 'y' axis.
         */
        private final int y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * @return The step in the 'x' axis.
         */
        public int getX() {
            return x;
        }

        /**
         * @return The step in the 'x' axis.
         */
        public int getY() {
            return y;
        }

        /**
         * An array of {@link Direction} ordered by the semi-circle convention.
         */
        private static Direction[] ANGLES_ORDER = {HORIZONTAL, TOP_RIGHT, VERTICAL, TOP_LEFT};

        /**
         * Finds the {@link Direction} corresponding to the given {@code angle}.
         *
         * @param angle The angle from which the {@link Direction} is built from.
         * @return The built {@link Direction}.
         */
        private static Direction fromAngle(double angle) {
            final double index = angle / (Math.PI / 4);
            if (Math.floor(index) != index) {
                // In this case the division is not an integer (which means that it is not divisible by 45º)
                throw new IllegalArgumentException("Angle must be divisible by 45º");
            }
            if (index < 0) {
                throw new IllegalArgumentException("The angle must be positive!");
            }
            if (index > 3) {
                throw new IllegalArgumentException("Max angle allowed is 135º");
            }
            return ANGLES_ORDER[(int) index];
        }
    }
}
