package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageOperationService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;


/**
 * Concrete implementation of {@link ImageOperationService}.
 */
@Service
public class ImageOperationServiceImpl implements ImageOperationService {

    @Override
    public Image sum(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 + v2);
    }

    @Override
    public Image subtract(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 - v2);
    }

    @Override
    public Image multiply(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 * v2);
    }

    @Override
    public Image dynamicRangeCompression(Image image) {
        final Double[] maximums = new MinAndMaxContainer(image).initialize().getMaximums();
        final double[] constants = IntStream.range(0, image.getBands())
                .mapToDouble(i -> 255.0 / Math.log10(1 + maximums[i]))
                .toArray();
        return createApplying(image, (x, y, b, v) -> constants[b] * Math.log10(1 + v));
    }

    @Override
    public Image gammaPower(Image image, double gamma) {
        final double constant = Math.pow(0xFF, 1 - gamma);
        return createApplying(image, (x, y, b, v) -> constant * Math.pow(v, gamma));
    }

    @Override
    public Image getNegative(Image image) {
        return createApplying(image, (x, y, i, value) -> 0xFF - value);
    }

    @Override
    public Image normalize(Image original) {
        final MinAndMaxContainer container = new MinAndMaxContainer(original).initialize();
        final Double[] minimums = container.getMinimums();
        final Double[] maximums = container.getMaximums();
        final double[] factors = IntStream.range(0, original.getBands())
                .mapToDouble(i -> 255 / (maximums[i] - minimums[i])).toArray();
        return createApplying(original, (x, y, i, value) -> (value - minimums[i]) * factors[i]);
    }


    // ================================================================================================================
    // Helper methods
    // ================================================================================================================

    /**
     * Performs a two {@link Image} operation, pixel by pixel,
     * applying the given {@link BiFunction} to generate the new pixel.
     *
     * @param first     The first {@link Image} in the operation.
     * @param second    The second {@link Image} in the operation.
     * @param operation A {@link BiFunction} that takes two pixel components (i.e samples),
     *                  being the 1st argument, the {@code first} {@link Image} pixel component,
     *                  the 2nd argument, the {@code second} {@link Image} pixel component,
     *                  and the result, the new value for the pixel component.
     * @return An {@link Image} whose samples will be the result of applying the operation.
     */
    private Image twoImagesPixelByPixelOperation(Image first, Image second,
                                                 BiFunction<Double, Double, Double> operation) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            throw new IllegalArgumentException("Both images must be the same size to be summed.");
        }
        return createApplying(first, (x, y, b, v) -> operation.apply(v, second.getSample(x, y, b)));
    }

    /**
     * Creates a new {@link Image} using as base the given {@code original} {@link Image},
     * applying the given {@code changer} {@link QuadFunction} to each sample.
     *
     * @param original The base {@link Image}.
     * @param changer  The {@link QuadFunction} to apply to each pixel,
     *                 being the first element, the row of the pixel being changed,
     *                 the second element, the column of the pixel being changed,
     *                 the third element, the band being changed,
     *                 and the fourth element, the original value in the row, column and band.
     *                 The function must return the changed value.
     * @return The new {@link Image}.
     */
    private static Image createApplying(Image original,
                                        QuadFunction<Integer, Integer, Integer, Double, Double> changer) {
        final int width = original.getWidth();
        final int height = original.getHeight();
        final int bands = original.getBands();
        final Image newImage = Image.trash(width, height, bands);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < bands; b++) {
                    final double value = original.getSample(x, y, b);
                    final double newValue = changer.apply(x, y, b, value);
                    newImage.setSample(x, y, b, newValue);
                }
            }
        }
        return newImage;
    }


    // ================================================================================================================
    // Helper classes
    // ================================================================================================================

    /**
     * Container class holding minimums and maximums values for a given {@link Image}.
     */
    private final static class MinAndMaxContainer {

        /**
         * An array holding the min. values.
         */
        private final Double[] minimums;

        /**
         * An array holding the max. values.
         */
        private final Double[] maximums;

        /**
         * The {@link Image} to which min. and max. values will be calculated, stored for lazy initialization.
         */
        private final Image image;

        /**
         * Constructor.
         *
         * @param image The {@link Image} to which min. and max. values will be calculated.
         */
        private MinAndMaxContainer(Image image) {
            this.minimums = image.getPixel(0, 0);
            this.maximums = image.getPixel(0, 0);
            this.image = image;
        }

        /**
         * Initializes this container.
         *
         * @return {@code this}, for method chaining.
         */
        private MinAndMaxContainer initialize() {
            populate(image, minimums, maximums);
            return this;
        }

        /**
         * @return The array containing the min. values.
         */
        private Double[] getMinimums() {
            return Arrays.copyOf(minimums, minimums.length);
        }

        /**
         * @return The array containing the max. values.
         */
        private Double[] getMaximums() {
            return Arrays.copyOf(maximums, maximums.length);
        }

        /**
         * Populates the given {@code minimums} and {@code maximums} arrays
         * with the minimums and maximums values for each band.
         *
         * @param image    The {@link Image} to which the calculation will be performed.
         * @param minimums An array to which the minimum value for each band will be saved.
         * @param maximums An array to which the maximum value for each band will be saved.
         * @throws IllegalArgumentException If any of the arrays are null, or if both arrays don'thave the same length.
         */
        private void populate(Image image, final Double[] minimums, final Double[] maximums)
                throws IllegalArgumentException {
            Assert.notNull(minimums, "The minimums array must not be null");
            Assert.notNull(maximums, "The maximums array must not be null");
            if (minimums.length != maximums.length) {
                throw new IllegalArgumentException("Both minimums and maximums array must have the same length");
            }
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int i = 0; i < image.getBands(); i++) {
                        final double value = image.getSample(x, y, i);
                        maximums[i] = maximums[i] > value ? maximums[i] : value;
                        minimums[i] = minimums[i] < value ? minimums[i] : value;
                    }
                }
            }
        }
    }
}
