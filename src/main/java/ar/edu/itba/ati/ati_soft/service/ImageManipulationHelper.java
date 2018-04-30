package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;
import ar.edu.itba.ati.ati_soft.utils.TriFunction;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Helper class that implements methods that help services.
 */
/* package */ class ImageManipulationHelper {


    /**
     * Normalizes the given {@link Image} to have pixel values between 0.0 and 255.0.
     *
     * @param original The original {@link Image}.
     * @return The normalized {@link Image}.
     */
    /* package */
    static Image normalize(Image original) {
        final MinAndMaxContainer container = new MinAndMaxContainer(original).initialize();
        final Double[] minimums = container.getMinimums();
        final Double[] maximums = container.getMaximums();
        final double[] factors = IntStream.range(0, original.getBands())
                .mapToDouble(i -> 255 / (maximums[i] - minimums[i])).toArray();
        return createApplying(original, (x, y, i, value) -> (value - minimums[i]) * factors[i]);
    }


    /**
     * Applies a threshold to the given {@link Image}, using the given threshold {@code value}.
     *
     * @param image The {@link Image} to which the threshold will be applied.
     * @param value The threshold value.
     * @return The threshold {@link Image}.
     * @apiNote This method expects the {@link Image} to be gray and normalized.
     */
    /* package */
    static Image threshold(Image image, int value) {
        return createApplying(image, (x, y, b, v) -> (double) (v <= value ? 0x0 : 0xFF));
    }

    /**
     * Converts the given {@link Image} into a gray image.
     *
     * @param image The {@link Image} to be converted.
     * @return The converted {@link Image}.+
     * @implNote This method uses the euclidean distance of each pixel
     * (being the space the one formed by all the image's bands).
     */
    /* package */
    static Image toGray(Image image) {
        return createApplying(image, (x, y, b, v) -> getEuclideanDistance(image.getPixel(x, y)));
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
    /* package */
    static Image createApplying(Image original, QuadFunction<Integer, Integer, Integer, Double, Double> changer) {
        return createApplying(original.getWidth(), original.getHeight(), original.getBands(),
                (x, y, b) -> changer.apply(x, y, b, original.getSample(x, y, b)));
    }

    /**
     * Creates a new {@link Image} with the given {@code width}, {@code height}, and {@code bands},
     * setting pixels using the given {@code pixelSetter} {@link TriFunction} to each sample.
     *
     * @param width       The {@link Image} width.
     * @param height      The {@link Image} height.
     * @param bands       The {@link Image} bands.
     * @param pixelSetter The {@link TriFunction} to apply to each pixel,
     *                    being the first element, the row of the pixel being set,
     *                    the second element, the column of the pixel being set,
     *                    the third element, the band being set,
     *                    The function must return the value to be set.
     * @return The new {@link Image}.
     */
    /* package */
    static Image createApplying(int width, int height, int bands,
                                TriFunction<Integer, Integer, Integer, Double> pixelSetter) {
        final Image newImage = Image.trash(width, height, bands);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < bands; b++) {
                    final double newValue = pixelSetter.apply(x, y, b);
                    newImage.setSample(x, y, b, newValue);
                }
            }
        }
        return newImage;
    }


    /**
     * Calculates the euclidean distance of the given {@code pixel}.
     *
     * @param pixel The pixel whose euclidean distance will be calculated.
     * @return The euclidean distance of the given {@code pixel}.
     */
    private static double getEuclideanDistance(Double[] pixel) {
        return Math.sqrt(Arrays.stream(pixel)
                .mapToDouble(i -> i)
                .map(p -> p * p)
                .sum());
    }

    /**
     * Container class holding minimums and maximums values for a given {@link Image}.
     */
    /* package */ final static class MinAndMaxContainer {

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
        /* package */ MinAndMaxContainer(Image image) {
            this.minimums = image.getPixel(0, 0);
            this.maximums = image.getPixel(0, 0);
            this.image = image;
        }

        /**
         * Initializes this container.
         *
         * @return {@code this}, for method chaining.
         */
        /* package */ MinAndMaxContainer initialize() {
            populate(image, minimums, maximums);
            return this;
        }

        /**
         * @return The array containing the min. values.
         */
        /* package */ Double[] getMinimums() {
            return Arrays.copyOf(minimums, minimums.length);
        }

        /**
         * @return The array containing the max. values.
         */
        /* package */ Double[] getMaximums() {
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
