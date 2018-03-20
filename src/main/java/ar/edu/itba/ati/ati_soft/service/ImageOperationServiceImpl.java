package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageOperationService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.QuadConsumer;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Concrete implementation of {@link ImageOperationService}.
 */
@Service
public class ImageOperationServiceImpl implements ImageOperationService {


    @Override
    public Image sum(Image first, Image second) {
        final BufferedImage firstContent = first.getContent();
        final BufferedImage secondContent = second.getContent();
        if (firstContent.getHeight() != secondContent.getHeight()
                || secondContent.getWidth() != secondContent.getWidth()) {
            throw new IllegalArgumentException("Both images must be the same size to be summed.");
        }
        final WritableRaster newRaster = createApplying(firstContent.getRaster(),
                (x, y, i, v) -> v + secondContent.getRaster().getSample(x, y, i),
                Integer[]::new,
                (r, x, y, p) -> r.setPixel(x, y, toPrimitiveArray(p)));
        final BufferedImage newContent = generateNewBufferedImage(getNormalized(newRaster), firstContent);
        return new Image(newContent);
    }

    @Override
    public Image getNegative(Image image) {
        final BufferedImage content = image.getContent();
        final WritableRaster newRaster = createApplying(content.getRaster(),
                (x, y, i, value) -> 0xFF - value,
                Integer[]::new,
                (r, x, y, p) -> r.setPixel(x, y, toPrimitiveArray(p)));
        final BufferedImage newContent = generateNewBufferedImage(newRaster, content);
        return new Image(newContent);
    }

    /**
     * Creates a {@link WritableRaster} from the given {@code raster}, applying the normalization function.
     *
     * @param raster The {@link Raster} to be normalized.
     * @return The new {@link WritableRaster} (i.e the given {@link Raster} with the normalization function applied).
     */
    private static WritableRaster getNormalized(Raster raster) {
        final int bands = raster.getNumDataElements();
        final int maximums[] = IntStream.range(0, bands).map(i -> 0).toArray();
        final int minimums[] = IntStream.range(0, bands).map(i -> 0).toArray();
        populateWithMinAndMax(raster, minimums, maximums);
        final double[] factors = IntStream.range(0, bands).mapToDouble(i -> 255 / (maximums[i] - minimums[i])).toArray();
        return createApplying(raster,
                (x, y, i, value) -> ((double) value - minimums[i]) * factors[i],
                Double[]::new,
                (r, x, y, p) -> r.setPixel(x, y, toPrimitiveArray(p)));
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
    private static void populateWithMinAndMax(Raster raster, final int minimums[], final int maximums[])
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
                    final int value = raster.getSample(x, y, i);
                    maximums[i] = maximums[i] > value ? maximums[i] : value;
                    minimums[i] = minimums[i] < value ? minimums[i] : value;
                }
            }
        }
    }

    /**
     * Creates a new {@link WritableRaster} using as base the given {@link Raster},
     * applying the given {@code changer} {@link BiFunction} to each pixel,
     * using the given {@code pixelGenerator} to create a pixel,
     * and using the given {@code setter} {@link QuadConsumer} to set values in a {@link WritableRaster}.
     *
     * @param raster         The base {@link Raster}.
     * @param changer        The {@link QuadFunction} to apply to each pixel,
     *                       being the first element, the row of the pixel being changed,
     *                       the second element, the column of the pixel being changed,
     *                       the third element, the band being changed,
     *                       and the fourth element, the original value in the row, column and band.
     *                       The function must return the changed value.
     * @param pixelGenerator An {@link IntFunction} that will generate pixels.
     * @param setter         A {@link QuadConsumer} which defines how a {@link WritableRaster} is set a {@code T} value.
     * @param <T>            Concrete type of value for each pixel.
     * @return The new {@link WritableRaster}.
     */
    private static <T> WritableRaster createApplying(Raster raster,
                                                     QuadFunction<Integer, Integer, Integer, Integer, T> changer,
                                                     IntFunction<T[]> pixelGenerator,
                                                     QuadConsumer<WritableRaster, Integer, Integer, T[]> setter) {
        final WritableRaster newRaster = Raster.createWritableRaster(raster.getSampleModel(), null);
        final int bands = raster.getNumDataElements();
        for (int x = 0; x < raster.getWidth(); x++) {
            for (int y = 0; y < raster.getHeight(); y++) {
                final T[] newPixel = pixelGenerator.apply(bands);
                for (int i = 0; i < bands; i++) {
                    final int value = raster.getSample(x, y, i);
                    newPixel[i] = changer.apply(x, y, i, value);
                }
                setter.accept(newRaster, x, y, newPixel);
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
