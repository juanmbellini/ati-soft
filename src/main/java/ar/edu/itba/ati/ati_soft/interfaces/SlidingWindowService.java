package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that is in charge of providing sliding window services.
 */
public interface SlidingWindowService {


    // ================================================================================================================
    // Filters
    // ================================================================================================================

    /**
     * Applies a mean filter to the given {@link Image}, using a window with the given {@code windowLength}.
     *
     * @param image        The {@link Image} to which the filter will be applied.
     * @param windowLength The length of the window.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyMeanFilter(Image image, int windowLength);

    /**
     * Applies a median filter to the given {@link Image}, using a window with the given {@code windowLength}.
     *
     * @param image        The {@link Image} to which the filter will be applied.
     * @param windowLength The length of the window.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyMedianFilter(Image image, int windowLength);

    /**
     * Applies a weight median filter to the given {@link Image}, using a window with the given {@code weights}.
     *
     * @param image   The {@link Image} to which the filter will be applied.
     * @param weights A two-dimensional array containing the weights.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyWeightMedianFilter(Image image, Integer[][] weights);

    /**
     * Applies a gaussian filter to the given {@link Image}, using the given {@code standardDeviation}.
     *
     * @param image             The {@link Image} to which the filter will be applied.
     * @param standardDeviation The standard deviation (i.e the sigma param) for the Gaussian function.
     * @return A new {@link Image} with the filter applied.
     * @
     */
    Image applyGaussianFilter(Image image, double standardDeviation);


    /**
     * Applies a Bilateral filtering to the given {@link Image}, using the given standard deviations.
     *
     * @param image      The {@link Image} to which the filter will be applied.
     * @param spatialStd The Gaussian standard deviation for the spatial domain filtering.
     * @param rangeStd   The Gaussian standard deviation for the range filtering.
     * @param windowSize The size of the window to be used.
     * @return a new {@link Image} with the filter applied.
     * @apiNote This filtering is done using the Gaussian function both for spatial domain and range filtering
     * (for more information,
     * see Tomasi C. , Manduchi R., (1998), Bilateral Filtering for Gray and Color Images, section 2.1).
     */
    Image applyBilateralFilter(Image image, double spatialStd, double rangeStd, int windowSize);


    // ================================================================================================================
    // Border detection
    // ================================================================================================================

    /**
     * Applies a high pass filter to the given {@link Image}, using a window with the given {@code windowLength}.
     *
     * @param image        The {@link Image} to which the filter will be applied.
     * @param windowLength The length of the window.
     * @return A new {@link Image} with the filter applied.
     */
    Image applyHighPassFilter(Image image, int windowLength);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Prewitt's gradient operator.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image prewittGradientOperatorBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Sobel's gradient operator.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image sobelGradientOperatorBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the anonymous mask's max direction.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image anonymousMaxDirectionBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Kirsh mask's max direction.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image kirshMaxDirectionBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Prewitt mask's max direction.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image prewittMaxDirectionBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Sobel mask's max direction.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image sobelMaxDirectionBorderDetectionMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Laplace's method.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @return The borders {@link Image}.
     */
    Image laplaceMethod(Image image);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Laplace's method,
     * using slope evaluation in order to accept or discard a zero cross.
     *
     * @param image          The {@link Image} to which the borders will be detected.
     * @param slopeThreshold The minimum slope in a zero cross.
     * @return The borders {@link Image}.
     */
    Image laplaceMethodWithSlopeEvaluation(Image image, double slopeThreshold);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Laplace of Gaussian method.
     *
     * @param image The {@link Image} to which the borders will be detected.
     * @param sigma The sigma value for the Gaussian.
     * @return The borders {@link Image}.
     */
    Image laplaceOfGaussianMethod(Image image, double sigma);

    /**
     * Returns a borders {@link Image} of the given {@code image}, applying the Laplace of Gaussian method,
     * using slope evaluation in order to accept or discard a zero cross.
     *
     * @param image          The {@link Image} to which the borders will be detected.
     * @param sigma          The sigma value for the Gaussian.
     * @param slopeThreshold The minimum slope in a zero cross.
     * @return The borders {@link Image}.
     */
    Image laplaceOfGaussianWithSlopeEvaluation(Image image, double sigma, double slopeThreshold);

    /**
     * Suppresses the no max. pixels in the given {@code Image}.
     *
     * @param image The {@link Image} to be processed.
     * @param sigma The sigma value to be used in the gaussian filter that is applied at the beginning of the method.
     * @return The processed {@link Image}.
     */
    Image suppressNoMaxPixels(Image image, double sigma);

    /**
     * Applies the Canny border detector.
     *
     * @param image The {@link Image} to be processed.
     * @param sigma The sigma value for the gaussian filter that is applied at the beginning of the method.
     * @return The borders {@link Image}.
     */
    Image cannyDetection(Image image, double sigma);

    /**
     * Applies the SUSAN border and corner detector.
     *
     * @param image The {@link Image} to be processed.
     * @param t     The t value.
     * @return The borders and corners {@link Image}.
     */
    Image susanDetection(Image image, double t);
}
