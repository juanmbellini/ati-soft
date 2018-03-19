package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that implements several methods that can change images.
 */
public interface ImageOperationService {


    /**
     * Calculates the negative {@link Image} for the given one.
     *
     * @param image The {@link Image} whose negative must be calculated.
     * @return The negative of the given {@link Image}.
     */
    Image getNegative(Image image);
}
