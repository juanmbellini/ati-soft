package ar.edu.itba.ati.ati_soft.models;

import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * An image representation that has a raster that stores data using {@code Double} values.
 */
public class Image {

    /**
     * The image width.
     */
    private final int width;

    /**
     * The image height.
     */
    private final int height;

    /**
     * The amount of values per pixel.
     */
    private final int bands;

    /**
     * A three-dimensional array holding raster data.
     */
    private final Double[][][] pixels;

    /**
     * Constructor.
     *
     * @param pixels A three-dimensional array holding raster data.
     */
    private Image(Double[][][] pixels) {
        // As the constructor is private, we are assuming that the pixels array is well formed.
        // If methods to build an Image is added, if it can't be assured that the array is well formed,
        // call the #validatePixelsArray(Double[][][]) method.
        this.width = pixels.length;
        this.height = pixels[0].length;
        this.bands = pixels[0][0].length;
        this.pixels = pixels;
    }


    /**
     * @return The raster width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The raster height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The amount of values per pixel.
     */
    public int getBands() {
        return bands;
    }

    /**
     * Gets the pixel in the position 'x', 'y'
     *
     * @param x The 'x' position of the pixel.
     * @param y The 'y' position of the pixel.
     * @return The pixel in the given position.
     */
    public Double[] getPixel(int x, int y) {
        Assert.isTrue(x >= 0 && x < width, "Usage 'x' value out of range.");
        Assert.isTrue(y >= 0 && y < height, "Usage 'y' value out of range.");
        Double[] pixel = pixels[x][y];
        return Arrays.copyOf(pixel, pixel.length); // Copy value to avoid changing state from the outside.
    }

    /**
     * Gets the sample (i.e value in the given {@code band}) for the given position.
     *
     * @param x    The 'x' position of the pixel.
     * @param y    The 'y' position of the pixel.
     * @param band The band of the pixel (i.e the channel).
     * @return The selected sample.
     */
    public double getSample(int x, int y, int band) {
        Assert.isTrue(x >= 0 && x < width, "Usage 'x' value out of range.");
        Assert.isTrue(y >= 0 && y < height, "Usage 'y' value out of range.");
        Assert.isTrue(band >= 0 && band < bands, "Usage a band out of range.");
        return pixels[x][y][band];
    }

    /**
     * Sets the given {@code pixel} in the given position.
     *
     * @param x     The 'x' position.
     * @param y     The 'y' position.
     * @param pixel The pixel to be set.
     * @throws IllegalArgumentException If the pixel is null, empty, or if it's length is not the legal one.
     */
    public void setPixel(int x, int y, Double[] pixel) throws IllegalArgumentException {
        Assert.isTrue(x >= 0 && x < width, "Usage 'x' value out of range.");
        Assert.isTrue(y >= 0 && y < height, "Usage 'y' value out of range.");
        Assert.notNull(pixel, "The pixel must not be null.");
        Assert.notEmpty(pixel, "The pixel must not be empty.");
        Assert.isTrue(pixel.length == bands, "The pixel must have " + bands + " bands.");
        this.pixels[x][y] = pixel;
    }

    /**
     * Sets the given {@code pixel} in the given position.
     *
     * @param x      The 'x' position.
     * @param y      The 'y' position.
     * @param band   The band of the pixel (i.e the channel).
     * @param sample The sample to be set.
     */
    public void setSample(int x, int y, int band, double sample) throws IllegalArgumentException {
        Assert.isTrue(x >= 0 && x < width, "Usage 'x' value out of range.");
        Assert.isTrue(y >= 0 && y < height, "Usage 'y' value out of range.");
        Assert.isTrue(band >= 0 && band < bands, "Usage a band out of range.");
        this.pixels[x][y][band] = sample;
    }


    /**
     * Returns a sub-raster of this image.
     *
     * @param xInitial  The starting 'x' point of the image where the sub-raster will be taken.
     * @param yInitial  The starting 'y' point of the image where the sub-raster will be taken.
     * @param newWidth  The sub-raster width.
     * @param newHeight The sub-raster height.
     * @return A {@code Double[][][]} representing the sub-raster
     * @apiNote The resulting array is totally independent from this image.
     * Modifying the result value won't affect this image.
     */
    public Double[][][] getSubRaster(int xInitial, int yInitial, int newWidth, int newHeight) {
        Assert.isTrue(xInitial >= 0 && xInitial + newWidth <= this.width
                && yInitial >= 0 && yInitial + newHeight <= this.height, "Out of range arguments");
        return Arrays.stream(pixels)
                .skip(xInitial)
                .limit(newWidth)
                .map(array -> Arrays.stream(array)
                        .skip(yInitial)
                        .limit(newHeight)
                        .map(pixel -> Arrays.copyOf(pixel, pixel.length)) // Copy pixel to avoid changing from outside
                        .toArray(Double[][]::new))
                .toArray(Double[][][]::new);
    }


    /**
     * Returns a copy of this image.
     *
     * @return A new totally independent instance of this image.
     */
    public Image copy() {
        return new Image(getSubRaster(0, 0, width, height));
    }

    /**
     * Builds an empty {@link Image} (i.e all values in 0.0).
     *
     * @param width  The raster width.
     * @param height The raster height.
     * @param bands  The amount of values per pixel.
     * @return The built raster.
     */
    public static Image empty(int width, int height, int bands) {
        return homogeneous(width, height, bands, 0.0);
    }

    /**
     * Builds an {@link Image} setting the given {@code value} to each sample.
     *
     * @param width  The raster width.
     * @param height The raster height.
     * @param bands  The amount of values per pixel.
     * @param value  The value to be set.
     * @return The built raster.
     */
    public static Image homogeneous(int width, int height, int bands, double value) {
        final Double[][][] pixels = IntStream.range(0, width)
                .mapToObj(x -> IntStream.range(0, height)
                        .mapToObj(y -> IntStream.range(0, bands)
                                .mapToObj(band -> value)
                                .toArray(Double[]::new))
                        .toArray(Double[][]::new))
                .toArray(Double[][][]::new);
        return new Image(pixels); // Not validating the array as it is well formed.
    }

    /**
     * Creates an {@link Image} with unknown values.
     *
     * @param width  The raster width.
     * @param height The raster height.
     * @param bands  The amount of values per pixel.
     * @return The built {@link Image}.
     */
    public static Image trash(int width, int height, int bands) {
        return new Image(new Double[width][height][bands]); // Not validating the array as it is well formed.
    }

    /**
     * Creates an {@link Image} from the given three-dimensional array holding raster data.
     *
     * @param pixels The three-dimensional array holding raster data.
     * @return The created {@link Image}.
     * @throws IllegalArgumentException If the array is not valid (is not a cubic three-dimensional array,
     *                                  or has null/empty values/sub-arrays.
     */
    public static Image fromArray(Double[][][] pixels) throws IllegalArgumentException {
        validatePixelsArray(pixels);  // Sanity check
        return new Image(pixels);
    }


    /**
     * Validates the given {@code pixels} array.
     *
     * @param pixels The array to be validated.
     * @throws IllegalArgumentException If the array is not valid (is not a cubic three-dimensional array,
     *                                  or has null/empty values/sub-arrays.
     */
    private static void validatePixelsArray(Double[][][] pixels) throws IllegalArgumentException {
        Assert.notNull(pixels, "The pixels array must not be null.");
        Assert.notEmpty(pixels, "The pixels array must not be empty.");
        // Early validation of first pixel to get data from it.
        Assert.notNull(pixels[0], "The first row array must not be null.");
        Assert.notEmpty(pixels[0], "The first row array must not be empty.");
        Assert.notNull(pixels[0][0], "The first pixel must not be null.");
        Assert.notEmpty(pixels[0][0], "The first pixel must not be empty.");

        final int width = pixels.length;
        final int height = pixels[0].length;
        final int bands = pixels[0][0].length;

        for (int x = 0; x < width; x++) {
            final Double[][] column = pixels[x];
            Assert.notNull(column, "Row " + x + " has a null column array.");
            Assert.notEmpty(column, "Row " + x + " has an empty column array.");
            Assert.isTrue(column.length == height,
                    "Row " + x + " has length " + column.length + ", which is different to " + height + ".");
            for (int y = 0; y < height; y++) {
                final Double[] pixel = pixels[x][y];
                Assert.notNull(pixel, "Pixel (" + x + ", " + y + ") has a null column array.");
                Assert.notEmpty(pixel, "Pixel (" + x + ", " + y + ") has an empty column array.");
                Assert.isTrue(pixel.length == bands,
                        "Pixel (" + x + ", " + y + ") has length " + pixel.length + ", " +
                                "which is different to " + bands + ".");
            }

        }
    }
}
