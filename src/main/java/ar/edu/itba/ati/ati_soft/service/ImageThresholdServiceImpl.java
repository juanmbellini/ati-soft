package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageThresholdService;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Service;

/**
 * Concrete implementation of {@link ImageThresholdService}.
 */
@Service
public class ImageThresholdServiceImpl implements ImageThresholdService {

    @Override
    public Image manualThreshold(Image image, int value) {
        // Image must be normalized so the given threshold value is the real one.
        return ImageManipulationHelper.createApplying(ImageManipulationHelper.normalize(image),
                (x, y, b, v) -> (double) (v <= value ? 0x0 : 0xFF));
    }
}
