package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.SlidingWindowService;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Concrete implementation of {@link SlidingWindowService}.
 */
@Service
public class SlidingWindowServiceImpl implements SlidingWindowService {

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
        final int margin = (int) (standardDeviation * 2);
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

        return applyFilter(image, mask.length,
                array -> IntStream.range(0, array.length)
                        .mapToObj(x -> IntStream.range(0, array[x].length).mapToObj(y -> array[x][y] * mask[x][y]))
                        .flatMap(Function.identity())
                        .reduce(0.0, (o1, o2) -> o1 + o2));
    }

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

        return applyFilter(ImageManipulationHelper.toGray(image), mask.length,
                array -> IntStream.range(0, array.length)
                        .mapToObj(x -> IntStream.range(0, array[x].length).mapToObj(y -> array[x][y] * mask[x][y]))
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
}
