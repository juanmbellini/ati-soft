package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.DiffusionService;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.TriFunction;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Concrete implementation of {@link DiffusionService}.
 */
@Service
public class DiffusionServiceImpl implements DiffusionService {

    // ================================================================================================================
    // Filters
    // ================================================================================================================

    @Override
    public Image isotropicDiffusion(Image image, int t, double lambda) {
        return diffuse(image, t, d -> 1.0, lambda);
    }

    @Override
    public Image anisotropicDiffusionWithLeclerc(Image image, int t, double lambda, double sigma) {
        return diffuse(image, t, d -> Math.exp((-Math.pow(Math.abs(d), 2)) / (sigma * sigma)), lambda);
    }

    @Override
    public Image anisotropicDiffusionWithLorentz(Image image, int t, double lambda, double sigma) {
        return diffuse(image, t, d -> 1 / (((Math.pow(Math.abs(d), 2)) / (sigma * sigma)) + 1), lambda);
    }

    // ================================================================================================================
    // Helpers
    // ================================================================================================================

    /**
     * Performs the {@link Image} diffusin.
     *
     * @param image    The {@link Image} being diffused.
     * @param t        The amount of iterations.
     * @param detector The detector function.
     * @param lambda   The lambda used in the discrete equation.
     * @return The diffused {@link Image}.
     */
    private static Image diffuse(Image image, int t, Function<Double, Double> detector, double lambda) {

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bands = image.getBands();

        if (t == 0) {
            return image.copy(); // Copy image as this is the expected behaviour.
        }
        for (int i = 0; i < t; i++) {
            image = ImageManipulationHelper.createApplying(() -> Image.empty(width, height, bands),
                    diffusionFunction(image, detector, lambda));
        }
        return image; // Note that the returned image is a new instance
    }

    /**
     * Creates a diffusion {@link TriFunction} which takes an 'x', and 'y' and a 'b' of an {@link Image},
     * and performs the diffusion.
     *
     * @param image    The {@link Image} being diffused.
     * @param detector The detector function.
     * @param lambda   The lambda used in the discrete equation.
     * @return The diffusion {@link TriFunction}.
     */
    private static TriFunction<Integer, Integer, Integer, Double> diffusionFunction(Image image,
                                                                                    Function<Double, Double> detector,
                                                                                    double lambda) {
        return (x, y, b) -> {
            if (x == 0 || x == image.getWidth() - 1 || y == 0 || y == image.getHeight() - 1) {
                return 0d;
            }
            return calculateDiffusedPixel(image, x, y, b, detector, lambda);
        };
    }


    /**
     * Calculates a diffused pixel.
     *
     * @param image    The image from where pixels are taken.
     * @param x        The row where the pixel being modified belongs.
     * @param y        The column where the pixel being modified belongs.
     * @param b        The pixel's band being modified.
     * @param detector The detector function.
     * @param lambda   The lambda used in the discrete equation.
     * @return The diffused pixel.
     */
    private static double calculateDiffusedPixel(Image image, int x, int y, int b,
                                                 Function<Double, Double> detector, double lambda) {
        final double pixel = image.getSample(x, y, b);
        final double north = image.getSample(x + 1, y, b);
        final double south = image.getSample(x - 1, y, b);
        final double east = image.getSample(x, y + 1, b);
        final double west = image.getSample(x, y - 1, b);
        final double sum = Stream.of(north, south, east, west)
                .map(v -> v - pixel)
                .map(d -> d * detector.apply(d))
                .mapToDouble(Double::doubleValue)
                .sum();
        return pixel + lambda * sum;
    }
}
