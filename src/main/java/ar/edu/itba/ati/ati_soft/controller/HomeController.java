package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Controller class for the main view.
 */
@FXMLController
public class HomeController {

    /**
     * The {@link Logger} object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    /**
     * An {@link ImageFileService} to manipulate image files.
     */
    private final ImageFileService imageFileService;

    /**
     * The root {@link Node},
     * used to get the {@link javafx.scene.Scene} and {@link javafx.stage.Window} of the home view.
     */
    @FXML
    private Node root;

    /**
     * The {@link ImageView} that will show an image without any changes.
     */
    @FXML
    private ImageView beforeImageView;

    /**
     * The {@link ImageView} that will show an image with changes applied.
     */
    @FXML
    private ImageView afterImageView;

    @Autowired
    public HomeController(ImageFileService imageFileService) {
        this.imageFileService = imageFileService;
    }

    /**
     * Closes the application.
     */
    @FXML
    public void closeApplication() {
        LOGGER.debug("Closing application...");
        Platform.exit();
    }

    /**
     * Opens an image, using a {@link FileChooser}.
     */
    @FXML
    public void openImage() {
        LOGGER.debug("Opening image...");
        Optional.ofNullable(selectFile())
                .map(this::openImage)
                .ifPresent(image -> drawImage(image, beforeImageView));
    }

    /**
     * Opens the given {@code imageFile}.
     *
     * @param imageFile The {@link File} containing the image to be opened.
     * @return a {@link BufferedImage} instance containing the image data,
     * or {@code null} in case the image could not be opened.
     */
    private BufferedImage openImage(File imageFile) {
        try {
            return imageFileService.openImage(imageFile);
        } catch (UnsupportedImageFileException e) {
            LOGGER.debug("File is not an image");
            return null;
        } catch (IOException e) {
            LOGGER.debug("Could not open image");
            return null;
        }
    }

    /**
     * Draws the given {@link BufferedImage} into the given {@link ImageView}.
     *
     * @param image     The image to be drawn.
     * @param imageView The {@link ImageView} to which the image will be drawn.
     */
    private static void drawImage(BufferedImage image, ImageView imageView) {
        final WritableImage writableImage = new WritableImage(image.getWidth(), image.getHeight());
        final PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                pixelWriter.setArgb(i, j, image.getRGB(i, j));
            }
        }
        imageView.setImage(writableImage);
    }

    /**
     * Makes the user select a file by the use of a {@link FileChooser}.
     *
     * @return The selected {@link File}.
     */
    private File selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select image");
        configureImageFileChooser(fileChooser);
        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    /**
     * Performs basic configuration of the given {@link FileChooser}, in order to be used to select images.
     *
     * @param fileChooser The {@link FileChooser} to be configured.
     */
    private static void configureImageFileChooser(FileChooser fileChooser) {
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Portable pixmap (.ppm)", "*.ppm"),
                new FileChooser.ExtensionFilter("Portable graymap (.pgm)", "*.pgm")
        );
    }
}
