package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.NoiseGenerationService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.RandomUtils;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of {@link NoiseGenerationService}.
 */
@Service
public class NoiseGenerationServiceImpl implements NoiseGenerationService {

    @Override
    public Image additiveGaussianNoise(Image image, double mean, double standardDeviation) {
        return ImageManipulationHelper.createApplying(image,
                (x, y, b, v) -> v + RandomUtils.randomGauss(mean, standardDeviation));
    }

    @Override
    public Image multiplicativeRayleighNoise(Image image, double scale) {
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> v * RandomUtils.randomRayleigh(scale));
    }

    @Override
    public Image multiplicativeExponentialNoise(Image image, double rate) {
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> v * RandomUtils.randomExponential(rate));
    }
}
