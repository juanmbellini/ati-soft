package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.NoiseGenerationService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.service.ImageManipulationHelper.MinAndMaxContainer;
import ar.edu.itba.ati.ati_soft.utils.RandomUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Random;

/**
 * Concrete implementation of {@link NoiseGenerationService}.
 */
@Service
public class NoiseGenerationServiceImpl implements NoiseGenerationService {

    @Override
    public Image additiveGaussianNoise(Image image, double mean, double standardDeviation, double density) {
        return ImageManipulationHelper.createApplying(image,
                (x, y, b, v) -> {
                    final double toAdd = new Random().nextDouble() <= density ?
                            RandomUtils.randomGauss(mean, standardDeviation) : 0;
                    return v + toAdd;
                });
    }

    @Override
    public Image multiplicativeRayleighNoise(Image image, double scale, double density) {
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> {
            final double toMultiply = new Random().nextDouble() <= density ?
                    RandomUtils.randomRayleigh(scale) : 1;
            return v * toMultiply;
        });
    }

    @Override
    public Image multiplicativeExponentialNoise(Image image, double rate, double density) {
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> {
            final double toMultiply = new Random().nextDouble() <= density ?
                    RandomUtils.randomExponential(rate) : 1;
            return v * toMultiply;
        });
    }

    @Override
    public Image saltAndPepperNoise(Image image, double p0, double p1) {
        Assert.isTrue(p0 >= 0.0 && p0 <= 1.0 && p1 >= 0.0 && p1 <= 1.0,
                "The p0 and p1 values must be between 0.0 and 1.0");
        Assert.isTrue(p0 < p1, "The value of p0 must be lower than the value of p1");
        final MinAndMaxContainer minAndMaxContainer = new MinAndMaxContainer(image).initialize();
        final Double[] minimums = minAndMaxContainer.getMinimums();
        final Double[] maximums = minAndMaxContainer.getMaximums();
        return ImageManipulationHelper.createApplying(image,
                (x, y, b, v) -> generateSaltOrPepper(p0, p1, minimums[b], maximums[b], v));
    }

    /**
     * Generates a salt (max) or a pepper(min) value, according to chance.
     *
     * @param p0           The upper limit up to which the pepper value will be returned.
     * @param p1           The lower limit from which the salt value will be returned.
     * @param min          The pepper value (typically, {@code 0x0}).
     * @param max          The salt value (typically, {@code 0x0}).
     * @param defaultValue The default value (i.e the returned value in case the random variate is between limits).
     * @return The salt or pepper value.
     */
    private static double generateSaltOrPepper(double p0, double p1, double min, double max, double defaultValue) {
        final double random = new Random().nextDouble();
        return random <= p0 ? min : random >= p1 ? max : defaultValue;
    }
}
