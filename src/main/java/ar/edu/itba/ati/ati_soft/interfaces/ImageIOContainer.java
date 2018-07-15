package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;
import com.github.jaiimageio.impl.common.ImageUtil;
import org.springframework.util.Assert;

import java.awt.image.*;
import java.util.Hashtable;
import java.util.function.Predicate;

/**
 * Bean class that holds together an {@link Image},
 * and data taken from a {@link BufferedImage} used to build the said {@link Image}.
 */
public final class ImageIOContainer {

    /**
     * The wrapped {@link Image}.
     */
    private final Image image;

    /**
     * The wrapped {@link SampleModel}.
     */
    private final SampleModel sampleModel;

    /**
     * The wrapped {@link ColorModel}.
     */
    private final ColorModel colorModel;

    /**
     * The wrapped properties {@link Hashtable}.
     */
    private final Hashtable<String, Object> properties;

    /**
     * Constructor.
     *
     * @param image       The {@link Image} to be wrapped.
     * @param sampleModel The {@link SampleModel} to be wrapped.
     * @param colorModel  The {@link ColorModel} to be wrapped.
     * @param properties  The properties {@link Hashtable} to be wrapped.
     */
    public ImageIOContainer(Image image, SampleModel sampleModel, ColorModel colorModel,
                            Hashtable<String, Object> properties) {
        this.image = image;
        this.sampleModel = sampleModel;
        this.colorModel = colorModel;
        this.properties = new Hashtable<>(properties);
    }

    /**
     * @return The wrapped {@link Image}.
     */
    public Image getImage() {
        return image;
    }

    /**
     * @return The wrapped {@link SampleModel}.
     */
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * @return The wrapped {@link ColorModel}.
     */
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * @return The wrapped properties {@link Hashtable}.
     */
    public Hashtable<String, Object> getProperties() {
        return properties;
    }

    /**
     * Creates a new instance, wrapping the given {@link Image}.
     *
     * @param image The new {@link Image} to be wrapped.
     * @return The new instance.
     */
    public ImageIOContainer buildForNewImage(Image image) {
        final SampleModel newSampleModel = sampleModel.createCompatibleSampleModel(image.getWidth(), image.getHeight());
        return new ImageIOContainer(image, newSampleModel, colorModel, properties);
    }

    /**
     * Builds a simple {@link ImageIOContainer} for a color image.
     *
     * @param image The {@link Image}.
     * @return The build {@link ImageIOContainer}.
     * @throws IllegalArgumentException In case the given {@code image} is not RGB (i.e 3 bands).
     */
    public static ImageIOContainer buildForNewColorImage(Image image) {
        return build(image, bands -> bands == 3, "Only RGB images are supported.");
    }

    /**
     * Builds a simple {@link ImageIOContainer} for a synthetic image.
     *
     * @param image The synthetic {@link Image}.
     * @return The built {@link ImageIOContainer}.
     * @throws IllegalArgumentException In case the given {@code image} is not Gray or RGB (i.e 1 band or 3 bands).
     */
    public static ImageIOContainer buildForSyntheticImage(Image image) {
        return build(image, bands -> bands == 1 || bands == 3, "Only Gray or RGB images are supported.");
    }

    /**
     * Builds a simple {@link ImageIOContainer}, checking the amount of bands with the given {@code bandsValidator}.
     *
     * @param image             The {@link Image}.
     * @param bandsValidator    A {@link Predicate} that takes the amount of bands the given {@code image} has,
     *                          and validates if that amount of bands is valid or not.
     *                          It must return {@code true} in case it is valid, or {@code false} otherwise.
     * @param bandsErrorMessage A message to be used in the thrown {@link IllegalArgumentException}
     *                          in case the amount of bands of the given {@code image} is not valid.
     * @return The build {@link ImageIOContainer}.
     * @throws IllegalArgumentException In case the given {@code bandsValidator} returns {@code false}.
     */
    private static ImageIOContainer build(Image image, Predicate<Integer> bandsValidator, String bandsErrorMessage)
            throws IllegalArgumentException {
        Assert.notNull(image, "The image must not be null");
        final int bands = image.getBands();
        Assert.isTrue(bandsValidator.test(bands), bandsErrorMessage);
        final SampleModel sampleModel = createSampleModel(image);
        final ColorModel colorModel = ImageUtil.createColorModel(sampleModel);
        return new ImageIOContainer(image, sampleModel, colorModel, new Hashtable<>());
    }


    /**
     * Builds a {@link SampleModel} for the given {@code image}.
     *
     * @param image The {@link Image} to which a {@link SampleModel} will be built.
     * @return The built {@link SampleModel}.
     */
    private static SampleModel createSampleModel(Image image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bands = image.getBands();
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bandOffsets.length; i++) {
            bandOffsets[i] = bands - 1 - i;
        }
        return new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, bands, bands * width, bandOffsets);
    }
}
