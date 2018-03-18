package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.FileHelper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A concrete implementation of the {@link ImageFileService}.
 */
@Service
public class ImageFileServiceImpl implements ImageFileService, InitializingBean {

    /**
     * {@link Map} containing the format for each supported extension.
     */
    private final Map<String, String> extensionFormats;

    /**
     * A {@link Set} containing the supported extensions by this service.
     */
    private final Map<String, String> supportedExtensions;

    public ImageFileServiceImpl() {
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
    public Image openImage(File imageFile) throws IOException, UnsupportedImageFileException {
        Assert.notNull(imageFile, "The image file must not be null");
        final String extension = FileHelper.getExtension(imageFile);
        validateExtension(extension);
        return Optional.ofNullable(ImageIO.read(imageFile))
                .map(Image::new)
                .orElseThrow(() -> new UnsupportedImageFileException("Could not decode the given file into an image"));
    }

    @Override
    public void saveImage(Image image, File file) throws IOException, UnsupportedImageFileException {
        Assert.notNull(file, "The image file must not be null");
        final String extension = FileHelper.getExtension(file);
        validateExtension(extension);
        ImageIO.write(image.getContent(), getFormat(extension), file);
    }

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
}
