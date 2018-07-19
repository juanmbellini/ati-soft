package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.util.Assert;

/**
 * Class implementing several methods to change the color space of an {@link Image}.
 */
/* package */ class ColorHelper {

    // ========================
    // D65 standard referent
    // ========================

    private static final double D65_X = 0.950470;
    private static final double D65_Y = 1.0;
    private static final double D65_Z = 1.088830;

    /**
     * Creates a new {@link Image} identical to the given {@code originalImage},
     * changing the color space from RGB to CIE-LAB.
     *
     * @param originalImage The original {@link Image} (must be an RGB image).
     * @return The converted {@link Image}.
     * @apiNote Even though {@link Image}s are generic
     * (meaning, there is no control over what type of data each channel has),
     * this method assumes that the given {@code originalImage} is an RGB image.
     */
    /* package */
    static Image rgbToCieLab(Image originalImage) {
        Assert.isTrue(originalImage.getBands() == 3,
                "The amount of bands of the original image must be 3 " +
                        "(and must represent each channel of a CIE-Lab image)");
        final int width = originalImage.getWidth();
        final int height = originalImage.getHeight();

        return ImageManipulationHelper.createApplying(() -> Image.empty(width, height, 3),
                (row, col) -> {
                    final Double[] rgbPixel = originalImage.getPixel(row, col);

                    // Normalize RGB values
                    final double r = rgbPixel[0] / 255.0;
                    final double g = rgbPixel[1] / 255.0;
                    final double b = rgbPixel[2] / 255.0;

                    // Map RGB to CIE XYZ
                    final double improvedR = r <= 0.04045 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
                    final double improvedG = g <= 0.04045 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
                    final double improvedB = b <= 0.04045 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

                    // Apply D65 referent
                    final double x = (0.4124564 * improvedR + 0.3575761 * improvedG + 0.1804375 * improvedB) / D65_X;
                    final double y = (0.2126729 * improvedR + 0.7151522 * improvedG + 0.0721750 * improvedB) / D65_Y;
                    final double z = (0.0193339 * improvedR + 0.1191920 * improvedG + 0.9503041 * improvedB) / D65_Z;

                    // Map CIE XYZ to CIE Lab
                    final double improvedX = x > 0.008856 ? Math.pow(x, 1.0 / 3) : 7.787037 * x + 4.0 / 29;
                    final double improvedY = y > 0.008856 ? Math.pow(y, 1.0 / 3) : 7.787037 * y + 4.0 / 29;
                    final double improvedZ = z > 0.008856 ? Math.pow(z, 1.0 / 3) : 7.787037 * z + 4.0 / 29;

                    return new Double[]{
                            116 * improvedY - 16,           // L
                            500 * (improvedX - improvedY),  // a
                            200 * (improvedY - improvedZ),  // b
                    };
                });
    }

    /**
     * Creates a new {@link Image} identical to the given {@code originalImage},
     * changing the color space from CIE-LAB to RGB.
     *
     * @param originalImage The original {@link Image} (must be an RGB image).
     * @return The converted {@link Image}.
     * @apiNote Even though {@link Image}s are generic
     * (meaning, there is no control over what type of data each channel has),
     * this method assumes that the given {@code originalImage} is an CIE-LAB image.
     */
    /* package */
    static Image cieLabToRGB(Image originalImage) {
        Assert.isTrue(originalImage.getBands() == 3,
                "The amount of bands of the original image must be 3 " +
                        "(and must represent each channel of a CIE-Lab image)");
        final int width = originalImage.getWidth();
        final int height = originalImage.getHeight();

        return ImageManipulationHelper.createApplying(() -> Image.empty(width, height, 3),
                (row, col) -> {
                    final Double[] cieLabPixel = originalImage.getPixel(row, col);
                    final double l = cieLabPixel[0];
                    final double a = cieLabPixel[1];
                    final double b = cieLabPixel[2];

                    // Map CIE Lab to CIE XYZ
                    final double y = (l + 16) / 116;
                    final double x = y + a / 500;
                    final double z = y - b / 200;

                    // Apply D65 referent
                    final double improvedX = D65_X * (x > 0.206893034 ? x * x * x : (x - 4.0 / 29) / 7.787037);
                    final double improvedY = D65_Y * (y > 0.206893034 ? y * y * y : (y - 4.0 / 29) / 7.787037);
                    final double improvedZ = D65_Z * (z > 0.206893034 ? z * z * z : (z - 4.0 / 29) / 7.787037);

                    // Map CIE XYZ to RGB
                    final double red = 3.2404542 * improvedX - 1.5371385 * improvedY - 0.4985314 * improvedZ;
                    final double green = -0.9692660 * improvedX + 1.8760108 * improvedY + 0.0415560 * improvedZ;
                    final double blue = 0.0556434 * improvedX - 0.2040259 * improvedY + 1.0572252 * improvedZ;
                    final double improvedR = red <= 0.00304 ? 12.92 * red : 1.055 * Math.pow(red, 1 / 2.4) - 0.055;
                    final double improvedG = green <= 0.00304 ? 12.92 * green : 1.055 * Math.pow(green, 1 / 2.4) - 0.055;
                    final double improvedB = blue <= 0.00304 ? 12.92 * blue : 1.055 * Math.pow(blue, 1 / 2.4) - 0.055;

                    return new Double[]{
                            255d * improvedR,
                            255d * improvedG,
                            255d * improvedB,
                    };

                });
    }
}
