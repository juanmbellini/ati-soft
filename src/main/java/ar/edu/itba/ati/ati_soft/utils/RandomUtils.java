package ar.edu.itba.ati.ati_soft.utils;

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
     * @return The generated pseudo random number.
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
     * @return The generated pseudo random number.
     */
    public static double randomGauss(double mean, double standardDeviation, Random random) {
        return random.nextGaussian() * standardDeviation * mean;
    }
}
