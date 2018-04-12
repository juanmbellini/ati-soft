package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;
import com.github.jaiimageio.impl.common.ImageUtil;
import org.springframework.util.Assert;

import java.awt.image.*;
import java.util.Hashtable;

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
     * Builds a simple {@link ImageIOContainer} for a synthetic image.
     *
     * @param image The synthetic {@link Image}.
     * @return The built {@link ImageIOContainer}.
     */
    public static ImageIOContainer buildForSyntheticImage(Image image) {
        Assert.notNull(image, "The image must not be null");
        final int bands = image.getBands();
        Assert.isTrue(bands == 1 || bands == 3, "Only Gray or RGB images are supported.");
        final int width = image.getWidth();
        final int height = image.getHeight();
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bandOffsets.length; i++) {
            bandOffsets[i] = bands - 1 - i;
        }
        final SampleModel sampleModel =
                new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, bands, bands * width, bandOffsets);
        final ColorModel colorModel = ImageUtil.createColorModel(sampleModel);
        return new ImageIOContainer(image, sampleModel, colorModel, new Hashtable<>());
    }
}
