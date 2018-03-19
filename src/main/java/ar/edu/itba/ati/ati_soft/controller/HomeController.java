package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import ar.edu.itba.ati.ati_soft.models.Image;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
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
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Controller class for the main view.
 */
@FXMLController
public class HomeController {

    /**
     * The {@link Logger} object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    // ==============================================================================
    // Services
    // ==============================================================================

    /**
     * An {@link ImageFileService} to manipulate image files.
     */
    private final ImageFileService imageFileService;


    // ==============================================================================
    // UI Components
    // ==============================================================================

    /**
     * The root {@link Node},
     * used to get the {@link javafx.scene.Scene} and {@link javafx.stage.Window} of the home view.
     */
    @FXML
    private Node root;

    /**
     * The {@link MenuItem} for the "save" operation.
     */
    @FXML
    private MenuItem saveMenuItem;

    /**
     * The {@link MenuItem} for the "save as..." operation.
     */
    @FXML
    private MenuItem saveAsMenuItem;

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

    // ==============================================================================
    // Event handling
    // ==============================================================================


    // ==============================================================================
    // Fields
    // ==============================================================================

    /**
     * The {@link File} from where the image was opened.
     */
    private File openedImageFile;

    /**
     * The actual image being displayed.
     */
    private Image actualImage;
//
//    /**
//     * The initial {@link Image}
//     */
//    private Image initialImage;

    /**
     * The last saved {@link Image}.
     */
    private Image lastSaved;

    /**
     * A {@link Stack} holding {@link Image}s obtained.
     */
    private final Stack<Image> imageHistory;

    /**
     * {@link Stack} holding each undone image (i.e those that were undone).
     */
    private final Stack<Image> undoneImages;


    // ==============================================================================
    // Constructor & Initialization
    // ==============================================================================

    @Autowired
    public HomeController(ImageFileService imageFileService) {
        this.imageFileService = imageFileService;
        this.imageHistory = new Stack<>();
        this.undoneImages = new Stack<>();
    }

    @FXML
    private void initialize() {
//        final BooleanBinding notOpenedBinding = new BooleanBinding() {
//            @Override
//            protected boolean computeValue() {
//                return actualImage == null;
//            }
//        };
//        final BooleanBinding notModifiedBinding = new BooleanBinding() {
//            @Override
//            protected boolean computeValue() {
//                return actualImage == lastSaved;
//            }
//        };
//        this.saveMenuItem.disableProperty().bind(Bindings.or(notOpenedBinding, notModifiedBinding));
//        this.saveAsMenuItem.disableProperty().bind(notOpenedBinding);

        LOGGER.debug("Home controller initialized");
    }


    // ==============================================================================
    // Behaviour methods
    // ==============================================================================

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
                .map(Image::getContent)
                .ifPresent(image -> {
                    drawImage(image, beforeImageView);
                    drawImage(image, afterImageView);
                });
    }

    @FXML
    public void saveImage() {
        LOGGER.debug("Saving image...");
        validateSave();
        if (actualImage == lastSaved) {
            LOGGER.warn("No changes has been made to image.");
        }
        saveImage(openedImageFile);
    }

    @FXML
    public void saveImageAs() {
        LOGGER.debug("Saving image as...");
        validateSave();
        final FileChooser fileChooser = new FileChooser();
        final File initialDirectory = Optional.ofNullable(openedImageFile.getParentFile())
                .orElse(new File(System.getProperty("user.home")));
        fileChooser.setTitle("Save as...");
        fileChooser.setInitialDirectory(initialDirectory);
        fileChooser.setInitialFileName(openedImageFile.getName());
        final File newFile = fileChooser.showSaveDialog(root.getScene().getWindow());
        saveImage(newFile);
    }

    @FXML
    public void undo() {
        doUndo();
    }

    @FXML
    public void redo() {
        doRedo();
    }

    // ==============================================================================
    // Helper methods
    // ==============================================================================

    /**
     * Opens the given {@code imageFile}.
     *
     * @param imageFile The {@link File} containing the image to be opened.
     * @return a {@link BufferedImage} instance containing the image data,
     * or {@code null} in case the image could not be opened.
     */
    private Image openImage(File imageFile) {
        try {
            final Image image = imageFileService.openImage(imageFile);
            afterOpeningImage(image, imageFile);
            return image;
        } catch (UnsupportedImageFileException e) {
            LOGGER.debug("File is not an image");
            return null;
        } catch (IOException e) {
            LOGGER.debug("Could not open image");
            return null;
        }
    }

    /**
     * Performs the operations that must be done after an image is opened.
     *
     * @param image The opened {@link Image}.
     * @param file  The {@link File} from where the image was opened.
     */
    private void afterOpeningImage(Image image, File file) {
        this.actualImage = image;
        this.lastSaved = image;
        this.openedImageFile = file;
    }

    /**
     * Validates that the save operation can be performed.
     *
     * @throws IllegalStateException If the state of the system does not allow to perform a save operation.
     */
    private void validateSave() throws IllegalStateException {
        if (this.actualImage == null) {
            throw new IllegalStateException("No opened image.");
        }
    }

    /**
     * Performs the save operation, using the given {@link File}.
     *
     * @param file {@link File} where the {@link #actualImage} will be saved.
     */
    private void saveImage(File file) {
        try {
            imageFileService.saveImage(actualImage, file);
        } catch (UnsupportedImageFileException e) {
            LOGGER.debug("Something wrong had happened.");
        } catch (IOException e) {
            LOGGER.debug("Could not save image.");
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
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select image");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        final List<FileChooser.ExtensionFilter> extensionFilters = imageFileService.getSupportedFormats().entrySet()
                .stream()
                .map(e -> new FileChooser.ExtensionFilter(e.getValue() + " (." + e.getKey() + ")", "*." + e.getKey()))
                .collect(Collectors.toList());
        fileChooser.getExtensionFilters().addAll(extensionFilters);
        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    /**
     * Modifies the actual image, setting the given {@link Image} as the actual,
     * saving the ex-actual in the {@link #imageHistory} {@link Stack},
     * and clearing the {@link #undoneImages} {@link Stack}.
     *
     * @param newImage The new actual image.
     */
    private void modify(Image newImage) {
        this.imageHistory.push(this.actualImage);
        this.undoneImages.clear();
        this.actualImage = newImage;
    }

    /**
     * Performs the "undo" operation.
     */
    private void doUndo() {
        if (imageHistory.isEmpty()) {
            return;
        }
        this.undoneImages.push(this.actualImage);
        this.actualImage = this.imageHistory.pop();
    }

    /**
     * Performs the "redo" operation.
     */
    private void doRedo() {
        if (undoneImages.isEmpty()) {
            return;
        }
        this.imageHistory.push(this.actualImage);
        this.actualImage = this.undoneImages.pop();
    }

    // ==============================================================================
    // Helper classes
    // ==============================================================================

    // ...
}
