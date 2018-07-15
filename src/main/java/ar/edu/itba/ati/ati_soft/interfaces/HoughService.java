package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

/**
 * Defines behaviour for an object that implements the Hough transform method in {@link Image}s,
 * in order to detect several shapes.
 */
public interface HoughService {

    /**
     * Finds straight lines in the given {@code image}.
     *
     * @param image         The {@link Image} to be analyzed.
     * @param sigma         A sigma value used to detect borders before.
     * @param thetaStep     A step of the angles to be analyzed.
     * @param epsilon       An epsilon used to identify if a given pixel belongs to a line.
     * @param maxPercentage A max. percentage, used to threshold the count.
     * @return The shapes {@link Image}.
     */
    Image findStraightLines(Image image, double sigma, double thetaStep, double epsilon, double maxPercentage);

    /**
     * Finds circles in the given {@code image}.
     *
     * @param image         The {@link Image} to be analyzed.
     * @param sigma         A sigma value used to detect borders before.
     * @param epsilon       An epsilon used to identify if a given pixel belongs to a circle.
     * @param maxPercentage A max. percentage, used to threshold the count.
     * @return The shapes {@link Image}.
     */
    Image findCircles(Image image, double sigma, double epsilon, double maxPercentage);
}
