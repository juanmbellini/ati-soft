package ar.edu.itba.ati.ati_soft.interfaces;

import ar.edu.itba.ati.ati_soft.models.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Defines behaviour for a service that manages image files.
 */
public interface ImageIOService {

    /**
     * @return A {@link Map} containing the supported formats of the implementation of this interface,
     * together with name given to each format.
     */
    Map<String, String> getSupportedFormats();

    /**
     * Opens a {@link BufferedImage} from the given {@code fileName} path.
     *
     * @param fileName The path of the image file to be opened.
     * @return An ImageIO {@link BufferedImage} instance containing the image data.
     * @throws IOException                   If any IO error occurs while opening the image file.
     * @throws UnsupportedImageFileException In case the file with the given {@code fileName}
     *                                       is not a supported image file (or is not an image at all).
     */
    default BufferedImage openImage(String fileName) throws IOException, UnsupportedImageFileException {
        return openImage(new File(fileName));
    }

    /**
     * Opens a {@link BufferedImage} from the given {@code imageFile}.
     *
     * @param imageFile The image {@link File} to be opened.
     * @return An ImageIO {@link BufferedImage} instance containing the image data.
     * @throws IOException                   If any IO error occurs while opening the image file.
     * @throws UnsupportedImageFileException In case the file with the given {@code fileName}
     *                                       is not a supported image file (or is not an image at all).
     */
    BufferedImage openImage(File imageFile) throws IOException, UnsupportedImageFileException;

    /**
     * Saves the given {@link BufferedImage} into a file with the given {@code fileName}.
     *
     * @param image    The ImageIO {@link BufferedImage} to be saved.
     * @param fileName The path of the file.
     */
    default void saveImage(BufferedImage image, String fileName) throws IOException, UnsupportedImageFileException {
        saveImage(image, new File(fileName));
    }

    /**
     * Saves the given {@link Image} into the given file.
     *
     * @param image The ImageIO {@link BufferedImage} to be saved.
     * @param file  The {@link File} to which the {@link Image} will be saved.
     */
    void saveImage(BufferedImage image, File file) throws IOException, UnsupportedImageFileException;

    /**
     * Creates an {@link ImageIOContainer} from the given {@link BufferedImage}.
     *
     * @param image The {@link BufferedImage} from where data is taken.
     * @return The built {@link ImageIOContainer}.
     */
    ImageIOContainer fromImageIO(BufferedImage image);

    /**
     * Creates a {@link BufferedImage} from the given {@link ImageIOContainer}.
     *
     * @param container The {@link ImageIOContainer} from where data is taken.
     * @return The built {@link BufferedImage}.
     */
    BufferedImage toImageIO(ImageIOContainer container);
}
