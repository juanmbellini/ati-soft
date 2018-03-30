package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that implements several methods that can change images.
 */
public interface ImageOperationService {

    /**
     * Sums both images.
     *
     * @param first  The first image.
     * @param second The second image.
     * @return The sum of both images.
     */
    Image sum(Image first, Image second);

    /**
     * Subtracts the {@code second} image to the {@code first} one.
     *
     * @param first  The first image.
     * @param second The second image.
     * @return The result of the subtraction.
     */
    Image subtract(Image first, Image second);

    /**
     * Multiplies both images.
     *
     * @param first  The first image.
     * @param second The second image.
     * @return The result of the multiplication.
     */
    Image multiply(Image first, Image second);

    /**
     * Creates a new {@link Image} from the given one, applying the dynamic range compression transformation.
     *
     * @param image The base image.
     * @return a new {@link Image}
     * which results from the first one with the dynamic range compression transformation applied to it.
     */
    Image dynamicRangeCompression(Image image);


    /**
     * Calculates the negative {@link Image} for the given one.
     *
     * @param image The {@link Image} whose negative must be calculated.
     * @return The negative of the given {@link Image}.
     */
    Image getNegative(Image image);
}
