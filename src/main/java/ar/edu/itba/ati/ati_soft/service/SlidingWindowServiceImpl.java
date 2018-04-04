package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.SlidingWindowService;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.function.Function;

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
        final Image newImage = image.copy();// Image.trash(width, height, bands);
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
