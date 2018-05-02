package ar.edu.itba.ati.ati_soft.service;

import org.springframework.util.Assert;

import java.util.Arrays;

/**
 * Class implementing several helper methods for mask operations.
 */
/* package */ class MaskHelper {

    /**
     * Validates the given mask (i.e is square, does not have nulls, and its size is the given one).
     *
     * @param mask The mask to validate.
     * @throws IllegalArgumentException If the mask is invalid.
     */
    /* package */
    static void validateMask(Double[][] mask) throws IllegalArgumentException {
        Assert.notNull(mask, "The mask must not be null");
        Assert.notEmpty(mask, "The mask must not be empty");
        Assert.notNull(mask[0], "The mask's first row must not be null");
        Assert.notEmpty(mask[0], "The mask's first row must not be empty");
        final int width = mask.length;
        final int height = mask[0].length;
        Assert.isTrue(height == width, "The mask must be square.");
        for (int x = 0; x < width; x++) {
            final Double[] column = mask[x];
            Assert.notNull(column, "Row " + x + " has a null column array.");
            Assert.notEmpty(column, "Row " + x + " has an empty column array.");
            Assert.isTrue(column.length == height,
                    "Row " + x + " has length " + column.length + ", which is different to " + height + ".");
            for (int y = 0; y < height; y++) {
                Assert.notNull(mask[x][y], "Value (" + x + ", " + y + ") has a null column array.");
            }
        }
    }

    /**
     * Rotates the mask contained by the given {@link MaskContainer}, according to the given {@code turns}.
     * Each turn represents a 45 degrees rotation counterclockwise.
     *
     * @param maskContainer The {@link MaskContainer} that holds the mask to be rotated.
     * @param turns         The amount of turns (45 degrees) to be rotated.
     * @return The rotated mask.
     */
    /* package */
    static Double[][] rotate3x3Mask(MaskContainer maskContainer, int turns) {
        Assert.notNull(maskContainer, "The mask container must not be null");
        final Double[][] mask = maskContainer.getMask();
        validateMask(mask);
        Assert.isTrue(mask.length == 3, "The mask must be a 3x3 square matrix");

        final Double[][] rotated = copyMask(mask);
        final int realTurns = turns % 8;
        // Rotates up to 180 degrees. If more is requested, then mirror.
        final int rotationTurns = realTurns % 4;
        for (int i = 0; i < rotationTurns; i++) {
            rotate3x3Mask45Degrees(rotated);
        }
        return realTurns == rotationTurns ? rotated : mirrorMask(rotated);
    }

    /**
     * Mirrors the mask contained by the given {@link MaskContainer}, returning a new instance of the mask.
     *
     * @param maskContainer The {@link MaskContainer} that holds the mask to be mirrored.
     * @return The mirrored mask.
     * @apiNote Calling this method is the same as calling {@link #rotate3x3Mask(MaskContainer, int)} with 4 turns.
     */
    /* package */
    static Double[][] mirrorMask(MaskContainer maskContainer) {
        Assert.notNull(maskContainer, "The mask container must not be null");
        return mirrorMask(maskContainer.getMask());
    }

    /**
     * Copies the given {@code mask}.
     *
     * @param mask The mask to be copied.
     * @return A new instance that is equals to the given {@code mask}.
     * @apiNote This method expects the given {@code rotatedMask} to be valid (no nulls or empties).
     */
    private static Double[][] copyMask(Double[][] mask) {
        return Arrays.stream(mask)
                .map(arr -> Arrays.stream(arr).toArray(Double[]::new))
                .toArray(Double[][]::new);
    }

    /**
     * Rotates the given 3x3 matrix 45 degrees (i.e moves each position only once).
     *
     * @param rotatedMask The matrix to be rotated.
     * @apiNote This method expects the given {@code rotatedMask} to be a 3x3 matrix, with no nulls.
     */
    private static void rotate3x3Mask45Degrees(Double[][] rotatedMask) {
        double aux = rotatedMask[0][1];
        rotatedMask[0][1] = rotatedMask[0][2];
        rotatedMask[0][2] = rotatedMask[1][2];
        rotatedMask[1][2] = rotatedMask[2][2];
        rotatedMask[2][2] = rotatedMask[2][1];
        rotatedMask[2][1] = rotatedMask[2][0];
        rotatedMask[2][0] = rotatedMask[1][0];
        rotatedMask[1][0] = rotatedMask[0][0];
        rotatedMask[0][0] = aux;
    }

    /**
     * Mirrors the given mask, returning a new instance.
     *
     * @param mask The mask to be mirrored.
     * @return The mirrored mask.
     * @apiNote This method expects the given {@code rotatedMask} to be valid (no nulls or empties).
     */
    private static Double[][] mirrorMask(Double[][] mask) {
        return Arrays.stream(mask)
                .map(arr -> Arrays.stream(arr)
                        .map(d -> -d)
                        .toArray(Double[]::new))
                .toArray(Double[][]::new);
    }

    /**
     * Defines behaviour for an object that contains a mask.
     */
    /* package */ interface MaskContainer {

        /**
         * Returns the contained mask.
         *
         * @return The contained mask.
         */
        Double[][] getMask();
    }
}
