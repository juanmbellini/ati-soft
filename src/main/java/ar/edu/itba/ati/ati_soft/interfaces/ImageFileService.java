package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Defines behaviour for a service that manages image files.
 */
public interface ImageFileService {

    /**
     * Opens an image from the given {@code fileName} path.
     *
     * @param fileName The path of the image file to be opened.
     * @return A {@link BufferedImage} instance containing the image data.
     * @throws IOException                   If any IO error occurs while opening the image file.
     * @throws UnsupportedImageFileException In case the file with the given {@code fileName}
     *                                       is not a supported image file (or is not an image at all).
     */
    default Image openImage(String fileName) throws IOException, UnsupportedImageFileException {
        return openImage(new File(fileName));
    }

    /**
     * Opens an image from the given {@code imageFile}.
     *
     * @param imageFile The image {@link File} to be opened.
     * @return A {@link BufferedImage} instance containing the image data.
     * @throws IOException                   If any IO error occurs while opening the image file.
     * @throws UnsupportedImageFileException In case the file with the given {@code fileName}
     *                                       is not a supported image file (or is not an image at all).
     */
    Image openImage(File imageFile) throws IOException, UnsupportedImageFileException;

    void saveImage(String fileName);
}
