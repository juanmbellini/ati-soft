package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that can perform diffusion operations.
 */
public interface DiffusionService {

    /**
     * Applies isotropic diffusion to the given {@code image}.
     *
     * @param image  The {@link Image} to be filtered with isotropic diffusion.
     * @param t      The amount of iterations.
     * @param lambda The lambda value used in the discrete equation.
     * @return The filtered {@link Image}.
     */
    Image isotropicDiffusion(Image image, int t, double lambda);

    /**
     * Applies anisotropic diffusion to the given {@code image}, using the Leclerc's detector.
     *
     * @param image  The {@link Image} to be filtered with isotropic diffusion.
     * @param t      The amount of iterations.
     * @param lambda The lambda value used in the discrete equation.
     * @param sigma  The sigma value for the Leclerc detector.
     * @return The filtered {@link Image}.
     */
    Image anisotropicDiffusionWithLeclerc(Image image, int t, double lambda, double sigma);

    /**
     * Applies anisotropic diffusion to the given {@code image}, using the Lorentz's detector.
     *
     * @param image  The {@link Image} to be filtered with isotropic diffusion.
     * @param t      The amount of iterations.
     * @param lambda The lambda value used in the discrete equation.
     * @param sigma  The sigma value for the Lorentz detector.
     * @return The filtered {@link Image}.
     */
    Image anisotropicDiffusionWithLorentz(Image image, int t, double lambda, double sigma);
}
