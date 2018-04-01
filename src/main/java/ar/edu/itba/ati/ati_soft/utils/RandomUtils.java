package ar.edu.itba.ati.ati_soft.utils;

import org.springframework.util.Assert;

import java.util.Random;

/**
 * Helper class that implements methods to aid the pseudo random numbers generation.
 */
public class RandomUtils {

    /**
     * Generates pseudorandom numbers with Gaussian distribution.
     *
     * @param mean              The distribution's mean value (i.e the mu value).
     * @param standardDeviation The distribution's standard deviation (i.e the sigma value).
     * @return The generated pseudorandom number.
     * @implNote This method uses the Polar method from Box, Muller and Marsaglia,
     * performing the inverse normalization operation.
     */
    public static double randomGauss(double mean, double standardDeviation) {
        return randomGauss(mean, standardDeviation, new Random());
    }

    /**
     * Generates pseudorandom numbers with Gaussian distribution,
     * using as base generator the given {@link Random} instance.
     *
     * @param mean              The distribution's mean value (i.e the mu value).
     * @param standardDeviation The distribution's standard deviation (i.e the sigma value).
     * @param random            The {@link Random} instance to be used to generate the distribution.
     * @return The generated pseudorandom number.
     * @implNote This method uses the Polar method from Box, Muller and Marsaglia,
     * performing the inverse normalization operation.
     * @implNote This method uses Inverse Transformation Sampling,
     * using a random variate with uniform distribution between 0 (exclusive) and 1 (inclusive).
     */
    public static double randomGauss(double mean, double standardDeviation, Random random) {
        Assert.notNull(random, "The Random instance must not be null.");
        Assert.isTrue(standardDeviation > 0, "The standard deviation must be positive");
        return random.nextGaussian() * standardDeviation + mean;
    }

    /**
     * Generates pseudorandom numbers with Rayleigh distribution.
     *
     * @param xi The distribution's xi parameter.
     * @return The generated pseudorandom number.
     */
    public static double randomRayleigh(double xi) {
        return randomRayleigh(xi, new Random());
    }

    /**
     * Generates pseudorandom numbers with Rayleigh distribution,
     * using as base generator the given {@link Random} instance.
     *
     * @param xi     The distribution's scale parameter (i.e the xi value).
     * @param random The {@link Random} instance to be used to generate the distribution.
     * @return The generated pseudorandom number.
     * @implNote This method uses Inverse Transformation Sampling,
     * using a random variate with uniform distribution between 0 (exclusive) and 1 (inclusive).
     */
    public static double randomRayleigh(double xi, Random random) {
        Assert.notNull(random, "The Random instance must not be null.");
        Assert.isTrue(xi > 0, "The scale parameter must be positive");
        // Subtract Random#nextDouble to 1, in order to have a value between 0 (exclusive) and 1 (inclusive).
        final double randomVariate = 1 - random.nextDouble();
        return xi * Math.sqrt(-2 * Math.log(randomVariate));
    }

    /**
     * Generates pseudorandom numbers with Exponential distribution.
     *
     * @param lambda The distribution's rate parameter (i.e the lambda value).
     * @return The generated pseudorandom number.
     * @implNote This method uses Inverse Transformation Sampling,
     * using a random variate with uniform distribution between 0 (exclusive) and 1 (inclusive).
     */
    public static double randomExponential(double lambda) {
        return randomExponential(lambda, new Random());
    }

    /**
     * Generates pseudorandom numbers with Exponential distribution,
     * using as base generator the given {@link Random} instance.
     *
     * @param lambda The distribution's rate parameter (i.e the lambda value).
     * @return The generated pseudorandom number.
     * @implNote This method uses Inverse Transformation Sampling,
     * using a random variate with uniform distribution between 0 (exclusive) and 1 (inclusive).
     */
    public static double randomExponential(double lambda, Random random) {
        Assert.notNull(random, "The Random instance must not be null.");
        Assert.isTrue(lambda > 0, "The rate parameter must be positive");
        // Subtract Random#nextDouble to 1, in order to have a value between 0 (exclusive) and 1 (inclusive).
        final double randomVariate = 1 - random.nextDouble();
        return (-1 / lambda) * Math.log(randomVariate);
    }
}
