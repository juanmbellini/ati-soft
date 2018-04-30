package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Service in charge of providing threshold services.
 */
public interface ImageThresholdService {

    /**
     * Creates a new {@link Image} applying a given threshold.
     *
     * @param image The {@link Image} to which the threshold function will be applied.
     * @param value The threshold (i.e value that indicates where the separation is done).
     * @return The threshold {@link Image}.
     */
    Image manualThreshold(Image image, int value);

    /**
     * Creates a new {@link Image} applying the threshold function,
     * calculating the threshold value using global threshold method.
     *
     * @param image  The {@link Image} to which the threshold function will be applied.
     * @param deltaT The difference of threshold value that must occur between two iterations in order to stop.
     * @return The threshold {@link Image}.
     */
    Image globalThreshold(Image image, int deltaT);
}
