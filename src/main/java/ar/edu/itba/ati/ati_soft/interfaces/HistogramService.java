package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;

import java.util.Map;

/**
 * Defines behaviour for an object that can perform histogram operations.
 */
public interface HistogramService {

    /**
     * Calculates the {@link Histogram}s for each channel of the given {@link Image}.
     *
     * @param image The {@link Image} whose histograms will be calculated.
     * @return A {@link Map} containing, for each band, the corresponding {@link Histogram}.
     */
    Map<Integer, Histogram> getHistograms(Image image);

    /**
     * Calculates the cumulative distribution {@link Histogram}.
     *
     * @param histogram The base {@link Histogram}.
     * @return A new {@link Histogram} instance, which is the contains the cumulative distribution of the given one.
     */
    Histogram getCumulativeDistributionHistogram(Histogram histogram);

    /**
     * Increases the contrast of the image, applying contrast stretching.
     *
     * @param image The {@link Image} whose contrast will be increased.
     * @return A new {@link Image} with the contrast increased.
     */
    Image increaseContrast(Image image);

    /**
     * Returns an {@link Image} whose {@link Histogram}s are equalized.
     *
     * @param image The {@link Image} whose histograms will be equalized.
     * @return An {@link Image} with equalized {@link Histogram}s.
     */
    Image equalize(Image image);
}
