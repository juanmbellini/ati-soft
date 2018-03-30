package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageOperationService;
import ar.edu.itba.ati.ati_soft.models.DoubleRaster;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.image.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        final DoubleRaster raster = DoubleRaster.fromImageIORaster(image.getContent().getRaster());
        final Double[] maximums = new MinAndMaxContainer(raster).initialize().getMaximums();
        final double[] constants = IntStream.range(0, raster.getBands())
                .mapToDouble(i -> 255.0 / Math.log10(1 + maximums[i]))
                .toArray();
        return finalizeImageCreation(image, createApplying(raster, (x, y, b, v) -> constants[b] * Math.log10(1 + v)));
    }

    @Override
    public Image gammaPower(Image image, double gamma) {
        final DoubleRaster raster = DoubleRaster.fromImageIORaster(image.getContent().getRaster());
        final double constant = Math.pow(0xFF, 1 - gamma);
        return finalizeImageCreation(image, createApplying(raster, (x, y, b, v) -> constant * Math.pow(v, gamma)));
    }

    @Override
    public Image getNegative(Image image) {
        final DoubleRaster raster = DoubleRaster.fromImageIORaster(image.getContent().getRaster());
        return finalizeImageCreation(image, createApplying(raster, (x, y, i, value) -> 0xFF - value));
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
        final BufferedImage firstContent = first.getContent();
        final BufferedImage secondContent = second.getContent();
        if (firstContent.getHeight() != secondContent.getHeight()
                || secondContent.getWidth() != secondContent.getWidth()) {
            throw new IllegalArgumentException("Both images must be the same size to be summed.");
        }
        final DoubleRaster raster = DoubleRaster.fromImageIORaster(firstContent.getRaster());
        return finalizeImageCreation(first,
                createApplying(raster,
                        (x, y, b, v) -> operation.apply(v, (double) secondContent.getRaster().getSample(x, y, b))));
    }

    /**
     * Performs the last step of building a new {@link Image}, taking data from the first one,
     * and building it with the {@code newRaster}.
     *
     * @param original  The original {@link Image} from where data will be taken
     *                  (i.e properties and {@link SampleModel}).
     * @param newRaster The {@link DoubleRaster} containing the new samples.
     * @return The built {@link Image}.
     */
    private static Image finalizeImageCreation(Image original, DoubleRaster newRaster) {
        final BufferedImage originalContent = original.getContent();
        final WritableRaster finalRaster = fromDoubleRaster(getNormalized(newRaster), originalContent.getSampleModel());
        final BufferedImage newContent = generateNewBufferedImage(finalRaster, originalContent);
        return new Image(newContent);
    }

    /**
     * Creates a {@link WritableRaster} from the given {@code raster}, applying the normalization function.
     *
     * @param raster The {@link Raster} to be normalized.
     * @return The new {@link WritableRaster} (i.e the given {@link Raster} with the normalization function applied).
     */
    private static DoubleRaster getNormalized(DoubleRaster raster) {
        final MinAndMaxContainer container = new MinAndMaxContainer(raster).initialize();
        final Double[] minimums = container.getMinimums();
        final Double[] maximums = container.getMaximums();
        final double[] factors = IntStream.range(0, raster.getBands())
                .mapToDouble(i -> 255 / (maximums[i] - minimums[i])).toArray();
        return createApplying(raster, (x, y, i, value) -> (value - minimums[i]) * factors[i]);
    }


    /**
     * Generates a {@link WritableRaster} from the given {@link WritableRaster}, using the given {@link SampleModel}.
     *
     * @param raster      The {@link WritableRaster} from where data will be taken..
     * @param sampleModel THe {@link SampleModel} used to create the new {@link Raster}.
     * @return The created {@link WritableRaster}.
     */
    private static WritableRaster fromDoubleRaster(DoubleRaster raster, SampleModel sampleModel) {
        final WritableRaster newRaster = Raster.createWritableRaster(sampleModel, null);
        for (int x = 0; x < raster.getWidth(); x++) {
            for (int y = 0; y < raster.getHeight(); y++) {
                for (int i = 0; i < raster.getBands(); i++) {
                    final double value = raster.getSample(x, y, i);
                    newRaster.setSample(x, y, i, (byte) value);
                }
            }
        }
        return newRaster;
    }

    /**
     * Creates a new {@link DoubleRaster} using as base the given {@code raster},
     * applying the given {@code changer} {@link QuadFunction} to each pixel.
     *
     * @param raster  The base {@link DoubleRaster}.
     * @param changer The {@link QuadFunction} to apply to each pixel,
     *                being the first element, the row of the pixel being changed,
     *                the second element, the column of the pixel being changed,
     *                the third element, the band being changed,
     *                and the fourth element, the original value in the row, column and band.
     *                The function must return the changed value.
     * @return The new {@link DoubleRaster}.
     */
    private static DoubleRaster createApplying(DoubleRaster raster,
                                               QuadFunction<Integer, Integer, Integer, Double, Double> changer) {
        final int width = raster.getWidth();
        final int height = raster.getHeight();
        final int bands = raster.getBands();
        final DoubleRaster newRaster = DoubleRaster.getTrashRaster(width, height, bands);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < bands; b++) {
                    final double value = raster.getSample(x, y, b);
                    final double newValue = changer.apply(x, y, b, value);
                    newRaster.setSample(x, y, b, newValue);
                }
            }
        }
        return newRaster;
    }


    /**
     * Generates a new {@link BufferedImage}, using the given {@link WritableRaster},
     * and taking properties from the {@code base} {@link BufferedImage}.
     *
     * @param writableRaster The {@link WritableRaster} to be used to generate the new {@link BufferedImage}.
     * @param base           A {@link BufferedImage} from which properties will be taken.
     * @return The new {@link BufferedImage}.
     */
    private static BufferedImage generateNewBufferedImage(WritableRaster writableRaster, BufferedImage base) {
        final ColorModel colorModel = base.getColorModel();
        final boolean preMultiplied = base.isAlphaPremultiplied();
        final Hashtable<String, Object> properties = getProperties(base);
        return new BufferedImage(colorModel, writableRaster, preMultiplied, properties);
    }

    /**
     * Gets the properties of the given {@link BufferedImage}.
     *
     * @param bufferedImage The {@link BufferedImage} whose properties must be calculated.
     * @return The properties {@link Hashtable} of the given {@code bufferedImage}.
     */
    private static Hashtable<String, Object> getProperties(BufferedImage bufferedImage) {
        return Optional.ofNullable(bufferedImage.getPropertyNames())
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(name -> new AbstractMap.SimpleEntry<>(name, bufferedImage.getProperty(name)))
                .collect(Hashtable::new, (t, e) -> t.put(e.getKey(), e.getValue()), Hashtable::putAll);
    }


    /**
     * Container class holding minimums and maximums values for a given {@link DoubleRaster}.
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
         * The {@link DoubleRaster} to which min. and max. values will be calculated, stored for lazy initialization.
         */
        private final DoubleRaster raster;

        /**
         * Constructor.
         *
         * @param raster The {@link DoubleRaster} to which min. and max. values will be calculated.
         */
        private MinAndMaxContainer(DoubleRaster raster) {
            this.minimums = raster.getPixel(0, 0);
            this.maximums = raster.getPixel(0, 0);
            this.raster = raster;
        }

        /**
         * Initializes this container.
         *
         * @return {@code this}, for method chaining.
         */
        private MinAndMaxContainer initialize() {
            populate(raster, minimums, maximums);
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
         * @param raster   The {@link Raster} to which the calculation will be performed.
         * @param minimums An array to which the minimum value for each band will be saved.
         * @param maximums An array to which the maximum value for each band will be saved.
         * @throws IllegalArgumentException If any of the arrays are null, or if both arrays don'thave the same length.
         */
        private void populate(DoubleRaster raster, final Double[] minimums, final Double[] maximums)
                throws IllegalArgumentException {
            Assert.notNull(minimums, "The minimums array must not be null");
            Assert.notNull(maximums, "The maximums array must not be null");
            if (minimums.length != maximums.length) {
                throw new IllegalArgumentException("Both minimums and maximums array must have the same length");
            }
            final int bands = minimums.length; // Using minimums, but is the same to use maximums.
            for (int x = 0; x < raster.getWidth(); x++) {
                for (int y = 0; y < raster.getHeight(); y++) {
                    for (int i = 0; i < bands; i++) {
                        final double value = raster.getSample(x, y, i);
                        maximums[i] = maximums[i] > value ? maximums[i] : value;
                        minimums[i] = minimums[i] < value ? minimums[i] : value;
                    }
                }
            }
        }
    }
}
