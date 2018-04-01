package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;

/**
 * Helper class that implements methods that help services.
 */
/* package */ class ImageManipulationHelper {

    /**
     * Creates a new {@link Image} using as base the given {@code original} {@link Image},
     * applying the given {@code changer} {@link QuadFunction} to each sample.
     *
     * @param original The base {@link Image}.
     * @param changer  The {@link QuadFunction} to apply to each pixel,
     *                 being the first element, the row of the pixel being changed,
     *                 the second element, the column of the pixel being changed,
     *                 the third element, the band being changed,
     *                 and the fourth element, the original value in the row, column and band.
     *                 The function must return the changed value.
     * @return The new {@link Image}.
     */
    /* package */
    static Image createApplying(Image original,
                                QuadFunction<Integer, Integer, Integer, Double, Double> changer) {
        final int width = original.getWidth();
        final int height = original.getHeight();
        final int bands = original.getBands();
        final Image newImage = Image.trash(width, height, bands);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < bands; b++) {
                    final double value = original.getSample(x, y, b);
                    final double newValue = changer.apply(x, y, b, value);
                    newImage.setSample(x, y, b, newValue);
                }
            }
        }
        return newImage;
    }
}
