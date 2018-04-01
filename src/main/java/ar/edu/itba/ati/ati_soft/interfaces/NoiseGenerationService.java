package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that can add noise to an {@link Image}.
 */
public interface NoiseGenerationService {

    /**
     * Adds Additive Gaussian noise to the given {@link Image}.
     *
     * @param image             The {@link Image} to pollute.
     * @param mean              The mean (i.e mu parameter)
     *                          used to generate pseudorandom numbers with a Gaussian distribution.
     * @param standardDeviation The standard deviation (i.e sigma parameter)
     *                          used to generate pseudorandom numbers with a Gaussian distribution.
     * @return A new {@link Image} instance with Additive Gaussian noise added to it.
     */
    Image additiveGaussianNoise(Image image, double mean, double standardDeviation);

    /**
     * Adds Multiplicative Rayleigh noise to the given {@link Image}.
     *
     * @param image The {@link Image} to pollute.
     * @param scale The scale parameter (i.e the xi parameter)
     *              used to generate pseudorandom numbers with a Gaussian distribution.
     * @return A new {@link Image} instance with  Multiplicative Rayleigh noise added to it.
     */
    Image multiplicativeRayleighNoise(Image image, double scale);

    /**
     * Adds Multiplicative Exponential noise to the given {@link Image}.
     *
     * @param image The {@link Image} to pollute.
     * @param rate  The rate parameter (i.e the lambda parameter)
     *              used to generate pseudorandom numbers with a Gaussian distribution.
     * @return A new {@link Image} instance with Multiplicative Exponential noise added to it.
     */
    Image multiplicativeExponentialNoise(Image image, double rate);
}
