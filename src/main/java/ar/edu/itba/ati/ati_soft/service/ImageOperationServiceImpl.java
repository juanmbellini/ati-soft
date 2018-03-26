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
    public Image getNegative(Image image) {
        final BufferedImage content = image.getContent();
        final DoubleRaster raster = DoubleRaster.fromImageIORaster(content.getRaster());
        final DoubleRaster newRaster = createApplying(raster, (x, y, i, value) -> 0xFF - value);
        final WritableRaster finalRaster = fromDoubleRaster(newRaster, content.getSampleModel());
        final BufferedImage newContent = generateNewBufferedImage(finalRaster, content);
        return new Image(newContent);
    }

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
        final DoubleRaster newRaster = createApplying(DoubleRaster.fromImageIORaster(firstContent.getRaster()),
                (x, y, b, v) -> operation.apply(v, (double) secondContent.getRaster().getSample(x, y, b)));
        final WritableRaster finalRaster = fromDoubleRaster(getNormalized(newRaster), firstContent.getSampleModel());
        final BufferedImage newContent = generateNewBufferedImage(finalRaster, firstContent);
        return new Image(newContent);
    }

    /**
     * Creates a {@link WritableRaster} from the given {@code raster}, applying the normalization function.
     *
     * @param raster The {@link Raster} to be normalized.
     * @return The new {@link WritableRaster} (i.e the given {@link Raster} with the normalization function applied).
     */
    private static DoubleRaster getNormalized(DoubleRaster raster) {
        final int bands = raster.getBands();
        final double[] maximums = IntStream.range(0, bands).mapToDouble(i -> 0).toArray();
        final double[] minimums = IntStream.range(0, bands).mapToDouble(i -> 0).toArray();
        populateWithMinAndMax(raster, minimums, maximums);
        final double[] factors = IntStream.range(0, bands).mapToDouble(i -> 255 / (maximums[i] - minimums[i])).toArray();
        return createApplying(raster, (x, y, i, value) -> (value - minimums[i]) * factors[i]);
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
    private static void populateWithMinAndMax(DoubleRaster raster, final double[] minimums, final double[] maximums)
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
     * Transforms the given {@code original} array into a primitive array.
     *
     * @param original The original array to be copied.
     * @return The new Array.
     */
    private static int[] toPrimitiveArray(Integer[] original) {
        final int[] result = new int[original.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = original[i];
        }
        return result;
    }

    /**
     * Transforms the given {@code original} array into a primitive array.
     *
     * @param original The original array to be copied.
     * @return The new Array.
     */
    private static double[] toPrimitiveArray(Double[] original) {
        final double[] result = new double[original.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = original[i];
        }
        return result;
    }


    /**
     * Generates a new {@link BufferedImage}, using the given {@link WritableRaster},
     * and taking properties from the {@code base} {@link BufferedImage}.
     *
     * @param writableRaster The {@link WritableRaster} to be used to generate the new {@link BufferedImage}.
     * @param base           A {@link BufferedImage} from which properties will be taken.
     * @return The new {@link BufferedImage}.
     */
    private BufferedImage generateNewBufferedImage(WritableRaster writableRaster, BufferedImage base) {
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

}
