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
     * @param density           The amount (in percentage between 0.0 and 1.0) of noise to add to the {@code image}.
     * @return A new {@link Image} instance with Additive Gaussian noise added to it.
     */
    Image additiveGaussianNoise(Image image, double mean, double standardDeviation, double density);

    /**
     * Adds Multiplicative Rayleigh noise to the given {@link Image}.
     *
     * @param image   The {@link Image} to pollute.
     * @param scale   The scale parameter (i.e the xi parameter)
     *                used to generate pseudorandom numbers with a Gaussian distribution.
     * @param density The amount (in percentage between 0.0 and 1.0) of noise to add to the {@code image}.
     * @return A new {@link Image} instance with  Multiplicative Rayleigh noise added to it.
     */
    Image multiplicativeRayleighNoise(Image image, double scale, double density);

    /**
     * Adds Multiplicative Exponential noise to the given {@link Image}.
     *
     * @param image   The {@link Image} to pollute.
     * @param rate    The rate parameter (i.e the lambda parameter)
     *                used to generate pseudorandom numbers with a Gaussian distribution.
     * @param density The amount (in percentage between 0.0 and 1.0) of noise to add to the {@code image}.
     * @return A new {@link Image} instance with Multiplicative Exponential noise added to it.
     */
    Image multiplicativeExponentialNoise(Image image, double rate, double density);

    /**
     * Adds Salt and Pepper noise to the given {@link Image}.
     *
     * @param image The {@link Image} to pollute.
     * @param p0    The p0 value.
     * @param p1    The p1 value.
     * @return A new {@link Image} instance with Salt and Pepper noise added to it.
     */
    Image saltAndPepperNoise(Image image, double p0, double p1);
}
