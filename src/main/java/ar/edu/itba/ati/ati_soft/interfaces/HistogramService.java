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
}
