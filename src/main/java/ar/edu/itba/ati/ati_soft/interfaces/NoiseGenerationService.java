package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that can add noise to an {@link Image}.
 */
public interface NoiseGenerationService {

    /**
     * Adds Gaussian noise to the given {@link Image}.
     *
     * @param image             The {@link Image} to pollute.
     * @param mean              The mean (i.e mu parameter)
     *                          used to generate pseudorandom numbers with a Gaussian distribution.
     * @param standardDeviation The standard deviation (i.e sigma parameter)
     *                          used to generate pseudorandom numbers with a Gaussian distribution.
     * @return A new {@link Image} instance with Additive Gaussian noise added to it.
     */
    Image addAdditiveGaussianNoise(Image image, double mean, double standardDeviation);
}
