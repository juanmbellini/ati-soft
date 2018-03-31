package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that implements several methods that can change {@link Image}s.
 */
public interface ImageOperationService {

    /**
     * Sums both images.
     *
     * @param first  The first {@link Image}.
     * @param second The second {@link Image}.
     * @return The sum of both {@link Image}s.
     */
    Image sum(Image first, Image second);

    /**
     * Subtracts the {@code second} image to the {@code first} one.
     *
     * @param first  The first {@link Image}.
     * @param second The second {@link Image}.
     * @return The result of the subtraction.
     */
    Image subtract(Image first, Image second);

    /**
     * Multiplies both images.
     *
     * @param first  The first {@link Image}.
     * @param second The second {@link Image}.
     * @return The result of the multiplication.
     */
    Image multiply(Image first, Image second);

    /**
     * Creates a new {@link Image} from the given one, applying the dynamic range compression transformation.
     *
     * @param image The base {@link Image}.
     * @return a new {@link Image}
     * which results from the first one with the dynamic range compression transformation applied to it.
     */
    Image dynamicRangeCompression(Image image);

    /**
     * Calculates the gamma power function for the given {@link Image}.
     *
     * @param image The {@link Image} to which the gamma function will be calculated.
     * @param gamma The gamma value.
     * @return A new {@link Image} with the gamma power function applied.
     */
    Image gammaPower(Image image, double gamma);


    /**
     * Calculates the negative {@link Image} for the given one.
     *
     * @param image The {@link Image} whose negative must be calculated.
     * @return The negative of the given {@link Image}.
     */
    Image getNegative(Image image);

    /**
     * Applies the normalization function to the given {@link Image}.
     *
     * @param image The base {@link Image}, to which the normalization will be applied.
     * @return The result of the operation.
     */
    Image normalize(Image image);
}
