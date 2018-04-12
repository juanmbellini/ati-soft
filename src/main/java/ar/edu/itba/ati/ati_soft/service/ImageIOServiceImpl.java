package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageIOContainer;
import ar.edu.itba.ati.ati_soft.interfaces.ImageIOService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.FileHelper;
import ar.edu.itba.ati.ati_soft.utils.TriFunction;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A concrete implementation of the {@link ImageIOService}.
 */
@Service
public class ImageIOServiceImpl implements ImageIOService, InitializingBean {

    /**
     * {@link Map} containing the format for each supported extension.
     */
    private final Map<String, String> extensionFormats;

    /**
     * A {@link Set} containing the supported extensions by this service.
     */
    private final Map<String, String> supportedExtensions;

    /**
     * Constructor.
     */
    @Autowired
    public ImageIOServiceImpl() {
        this.supportedExtensions = new HashMap<>();
        this.extensionFormats = new HashMap<>();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.supportedExtensions.put("ppm", "Portable pixmap");
        this.supportedExtensions.put("pgm", "Portable graymap");

        this.extensionFormats.put("pgm", "pnm");
        this.extensionFormats.put("ppm", "pnm");
    }

    @Override
    public Map<String, String> getSupportedFormats() {
        return new HashMap<>(supportedExtensions);
    }

    @Override
    public BufferedImage openImage(File imageFile) throws IOException, UnsupportedImageFileException {
        Assert.notNull(imageFile, "The image file must not be null");
        final String extension = FileHelper.getExtension(imageFile);
        validateExtension(extension);
        return Optional.ofNullable(ImageIO.read(imageFile))
                .orElseThrow(() -> new UnsupportedImageFileException("Could not decode the given file into an image"));
    }

    @Override
    public void saveImage(BufferedImage image, File file) throws IOException, UnsupportedImageFileException {
        Assert.notNull(file, "The image file must not be null");
        final String extension = FileHelper.getExtension(file);
        validateExtension(extension);
        ImageIO.write(image, getFormat(extension), file);
    }

    @Override
    public ImageIOContainer fromImageIO(BufferedImage image) {
        final Image translated = createImage(image);
        final SampleModel sampleModel = image.getSampleModel();
        final ColorModel colorModel = image.getColorModel();
        final Hashtable<String, Object> properties = getProperties(image);
        return new ImageIOContainer(translated, sampleModel, colorModel, properties);
    }

    @Override
    public BufferedImage toImageIO(ImageIOContainer container) {
        final ColorModel colorModel = container.getColorModel();
        final WritableRaster raster = buildRaster(container.getImage(), container.getSampleModel());
        final Hashtable<String, Object> properties = container.getProperties();
        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), properties);
    }


    // ================================================================================================================
    // Helper methods
    // ================================================================================================================

    /**
     * Validates that the given {@code extension} is supported by this service.
     *
     * @param extension The {@code extension} to be validated.
     * @throws UnsupportedImageFileException If the extension is not supported.
     */
    private void validateExtension(String extension) throws UnsupportedImageFileException {
        if (!supportedExtensions.containsKey(extension)) {
            throw new UnsupportedImageFileException("Images with extension " + extension + " are not supported");
        }
    }

    /**
     * Gets the format for the given {@code extension}.
     *
     * @param extension The extension for which the format must be retrieved.
     * @return The format for the given {@code extension}.
     */
    private String getFormat(String extension) {
        return Optional.ofNullable(extensionFormats.get(extension))
                .orElseThrow(() -> new UnsupportedImageFileException("No format for the given extension"));
    }

    /**
     * Creates an {@link Image} from the given {@link BufferedImage}.
     *
     * @param image The {@link BufferedImage} from where data will be taken.
     * @return The {@link Image} built from the given {@link BufferedImage}.
     */
    private static Image createImage(BufferedImage image) {
        final Raster raster = image.getRaster();
        final Double[][][] pixels = IntStream.range(0, image.getWidth())
                .mapToObj(x -> IntStream.range(0, raster.getHeight())
                        .mapToObj(y -> IntStream.range(0, raster.getNumBands())
                                .mapToObj(band -> (double) raster.getSample(x, y, band))
                                .toArray(Double[]::new))
                        .toArray(Double[][]::new))
                .toArray(Double[][][]::new);
        return Image.fromArray(pixels);
    }

    /**
     * Gets the properties of the given {@link BufferedImage}.
     *
     * @param image The {@link BufferedImage} whose properties must be get.
     * @return The properties {@link Hashtable} of the given {@code image}.
     */
    private static Hashtable<String, Object> getProperties(BufferedImage image) {
        return Optional.ofNullable(image.getPropertyNames())
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(name -> new AbstractMap.SimpleEntry<>(name, image.getProperty(name)))
                .collect(Hashtable::new, (t, e) -> t.put(e.getKey(), e.getValue()), Hashtable::putAll);
    }

    /**
     * Builds a {@link WritableRaster} from the given {@link Image}, using the given {@link SampleModel}.
     *
     * @param image       The {@link Image} from where data will be taken..
     * @param sampleModel The {@link SampleModel} used to create the new {@link WritableRaster}.
     * @return The created {@link WritableRaster}.
     */
    private static WritableRaster buildRaster(Image image, SampleModel sampleModel) {
        return buildRaster(image::getWidth, image::getHeight, image::getBands, image::getSample, sampleModel);
    }

    /**
     * Builds a {@link WritableRaster}, using the given suppliers and {@link SampleModel}.
     *
     * @param widthSupplier  An {@link IntSupplier} that provides the width.
     * @param heightSupplier An {@link IntSupplier} that provides the height.
     * @param bandsSupplier  An {@link IntSupplier} that provides the amount of bands.
     * @param sampleSupplier A {@link TriFunction} that takes the coordinate (x, y, band) and returns the sample for it.
     * @param sampleModel    The {@link SampleModel} used to create the new {@link WritableRaster}.
     * @return The created {@link WritableRaster}.
     */
    private static WritableRaster buildRaster(IntSupplier widthSupplier, IntSupplier heightSupplier,
                                              IntSupplier bandsSupplier,
                                              TriFunction<Integer, Integer, Integer, Double> sampleSupplier,
                                              SampleModel sampleModel) {
        final int width = widthSupplier.getAsInt();
        final int height = heightSupplier.getAsInt();
        final int bands = bandsSupplier.getAsInt();
        final WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int b = 0; b < bands; b++) {
                    final double value = sampleSupplier.apply(x, y, b);
                    raster.setSample(x, y, b, (byte) value);
                }
            }
        }
        return raster;
    }
}
