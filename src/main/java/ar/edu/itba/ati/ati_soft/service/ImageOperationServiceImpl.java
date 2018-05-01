package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageOperationService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.service.ImageManipulationHelper.MinAndMaxContainer;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;
import java.util.stream.IntStream;


/**
 * Concrete implementation of {@link ImageOperationService}.
 */
@Service
public class ImageOperationServiceImpl implements ImageOperationService {

    @Override
    public Image sum(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 + v2);
    }

    @Override
    public Image subtract(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 - v2);
    }

    @Override
    public Image multiply(Image first, Image second) {
        return twoImagesPixelByPixelOperation(first, second, (v1, v2) -> v1 * v2);
    }

    @Override
    public Image multiplyByScalar(Image image, double scalar) {
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> scalar * v);
    }

    @Override
    public Image dynamicRangeCompression(Image image) {
        final Double[] maximums = new MinAndMaxContainer(image).initialize().getMaximums();
        final double[] constants = IntStream.range(0, image.getBands())
                .mapToDouble(i -> 255.0 / Math.log10(1 + maximums[i]))
                .toArray();
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> constants[b] * Math.log10(1 + v));
    }

    @Override
    public Image gammaPower(Image image, double gamma) {
        final double constant = Math.pow(0xFF, 1 - gamma);
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> constant * Math.pow(v, gamma));
    }

    @Override
    public Image getNegative(Image image) {
        // Image must be normalized as it can have pixels bigger than 0xFF
        return ImageManipulationHelper
                .createApplying(ImageManipulationHelper.normalize(image), (x, y, i, value) -> 0xFF - value);
    }

    @Override
    public Image normalize(Image original) {
        return ImageManipulationHelper.normalize(original);
    }


    // ================================================================================================================
    // Helper methods
    // ================================================================================================================

    /**
     * Performs a two {@link Image} operation, pixel by pixel,
     * applying the given {@link BiFunction} to generate the new pixel.
     *
     * @param first     The first {@link Image} in the operation.
     * @param second    The second {@link Image} in the operation.
     * @param operation A {@link BiFunction} that takes two pixel components (i.e samples),
     *                  being the 1st argument, the {@code first} {@link Image} pixel component,
     *                  the 2nd argument, the {@code second} {@link Image} pixel component,
     *                  and the result, the new value for the pixel component.
     * @return An {@link Image} whose samples will be the result of applying the operation.
     */
    private Image twoImagesPixelByPixelOperation(Image first, Image second,
                                                 BiFunction<Double, Double, Double> operation) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            throw new IllegalArgumentException("Both images must be the same size to be summed.");
        }
        return ImageManipulationHelper.createApplying(first, (x, y, b, v) -> operation.apply(v, second.getSample(x, y, b)));
    }
}
