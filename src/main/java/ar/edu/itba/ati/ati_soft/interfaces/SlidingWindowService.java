package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that is in charge of providing sliding window services.
 */
public interface SlidingWindowService {

    /**
     * Applies a mean filter to the given {@link Image}, using a window with the given {@code windowLength}.
     *
     * @param image        The {@link Image} to which the filter will be applied.
     * @param windowLength The length of the window.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyMeanFilter(Image image, int windowLength);

    /**
     * Applies a median filter to the given {@link Image}, using a window with the given {@code windowLength}.
     *
     * @param image        The {@link Image} to which the filter will be applied.
     * @param windowLength The length of the window.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyMedianFilter(Image image, int windowLength);

    /**
     * Applies a weight median filter to the given {@link Image}, using a window with the given {@code weights}.
     *
     * @param image   The {@link Image} to which the filter will be applied.
     * @param weights A two-dimensional array containing the weights.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyWeightMedianFilter(Image image, Integer[][] weights);

    /**
     * Applies a gaussian filter to the given {@link Image}, using the given {@code standardDeviation}.
     *
     * @param image             The {@link Image} to which the filter will be applied.
     * @param standardDeviation The standard deviation (i.e the sigma param) for the Gaussian function.
     * @return A new {@link Image} with the filter applied.
     * @
     */
    Image applyGaussianFilter(Image image, double standardDeviation);
}
