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
    public Image addAdditiveGaussianNoise(Image image, double mean, double standardDeviation) {
        return ImageManipulationHelper.createApplying(image,
                (x, y, b, v) -> v + RandomUtils.randomGauss(mean, standardDeviation));
    }
}
