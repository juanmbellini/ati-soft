package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Service in charge of providing threshold services.
 */
public interface ImageThresholdService {

    /**
     * Creates a new {@link Image} applying a given threshold.
     *
     * @param image The {@link Image} whose threshold must be calculated.
     * @param value The threshold (i.e value that indicates where the separation is done).
     * @return The threshold {@link Image}.
     */
    Image manualThreshold(Image image, int value);
}
