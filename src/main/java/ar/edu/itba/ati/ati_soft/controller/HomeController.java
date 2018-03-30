package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import ar.edu.itba.ati.ati_soft.interfaces.ImageOperationService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import ar.edu.itba.ati.ati_soft.models.Image;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    private final ImageOperationService imageOperationService;


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
     * The {@link ImageView} that will show an image with changes applied.
     */
    @FXML
    private ImageView imageView;

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

    /**
     * The initial {@link Image} (used for displaying it with the {@link #showOriginal()} method).
     */
    private Image initialImage;

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
    public HomeController(ImageFileService imageFileService, ImageOperationService imageOperationService) {
        this.imageFileService = imageFileService;
        this.imageOperationService = imageOperationService;
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
    // Controller methods
    // ==============================================================================

    // ======================================
    // File actions
    // ======================================

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
                .ifPresent(image -> drawImage(image, imageView));
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


    // ======================================
    // Edit actions
    // ======================================

    @FXML
    public void undo() {
        doUndo();
        drawActual();
    }

    @FXML
    public void redo() {
        doRedo();
        drawActual();
    }

    @FXML
    public void sum() {
        twoImagesOperationAction(imageOperationService::sum, "sum");
    }

    @FXML
    public void subtract() {
        twoImagesOperationAction(imageOperationService::subtract, "subtraction");
    }

    @FXML
    public void multiply() {
        twoImagesOperationAction(imageOperationService::multiply, "multiplication");
    }

    @FXML
    public void dynamicRangeCompression() {
        oneImageOperationAction(imageOperationService::dynamicRangeCompression, "dynamic range compression");
    }

    @FXML
    public void gammaPower() {
        getNumber("Gamma power", "", "Insert the gamma value", Double::parseDouble)
                .ifPresent(gamma ->
                        oneImageOperationAction(image -> imageOperationService.gammaPower(image, gamma),
                                "gamma power transformation"));
    }

    @FXML
    public void negative() {
        oneImageOperationAction(imageOperationService::getNegative, "negative calculation");
    }

    // ======================================
    // View actions
    // ======================================

    @FXML
    public void showOriginal() {
        final ImageView originalImageView = new ImageView();
        drawImage(initialImage.getContent(), originalImageView);
        originalImageView.preserveRatioProperty().setValue(true);
        final double width = 700;
        originalImageView.setFitWidth(width);
        final double height = originalImageView.getLayoutBounds().getHeight();
        final BorderPane borderPane = new BorderPane(originalImageView);
        final Scene scene = new Scene(borderPane, width, height);
        final Stage stage = new Stage();
        stage.setScene(scene);
        originalImageView.fitWidthProperty().bind(stage.widthProperty());
        originalImageView.fitHeightProperty().bind(stage.heightProperty());
        stage.show();
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
        this.initialImage = image;
        this.imageHistory.clear();
        this.undoneImages.clear();
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
     * Show ups a {@link javafx.scene.control.Dialog} that expects a numeric value to be inserted.
     * The transformation is performed using a {@link Function} that takes a {@link String}
     * and converts it into a {@link Number}.
     * In case the inserted value is not a number that can be created with the {@code converter},
     * an {@link javafx.scene.control.Alert.AlertType#ERROR} {@link Alert} is showed up.
     *
     * @param header       The message to be displayed in the {@link javafx.scene.control.Dialog} header.
     * @param defaultValue The default value for the {@link javafx.scene.control.Dialog}
     *                     (i.e what appears in the text field).
     * @param converter    A {@link Function} to be used to create the {@link Number}.
     * @param <N>          The concrete subtype of {@link Number} (e.g {@link Integer} or {@link Double}).
     * @return An {@link Optional} of {@code N} holding the inserted value,
     * or empty if no value, or no number was inserted.
     */
    private <N extends Number> Optional<N> getNumber(String title, String header, String defaultValue,
                                                     Function<String, N> converter) {
        final TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setHeaderText(header);
        textInputDialog.setTitle(title);
        return textInputDialog.showAndWait().map(value -> convertToNumber(value, converter));
    }

    /**
     * Converts the given {@code value} into a number, using the given {@code converter}.
     * In case the {@code value} is not a valid number
     * (i.e can't be converted into a number with the given {@code converter}),
     * an {@link Alert} {@link javafx.scene.control.Dialog} is displayed, and null is returned.
     *
     * @param value     The {@link String} to be converted into a number.
     * @param converter The {@link Function} to apply to the {@code value} to transform it into a number.
     * @param <N>       The concrete subtype of {@link Number} (e.g {@link Integer} or {@link Double}).
     * @return The converted value.
     */
    private <N extends Number> N convertToNumber(String value, Function<String, N> converter) {
        try {
            return converter.apply(value);
        } catch (NumberFormatException e) {
            final Alert alert = new Alert(Alert.AlertType.ERROR,
                    "The value \"" + value + "\" is not a number.");
            alert.setHeaderText("");
            alert.show();
            return null;
        }
    }

    /**
     * Performs the given {@code imageOperation}, applying it to the given {@link #actualImage}.
     * The result of the operation will be set a the new {@link #actualImage}.
     *
     * @param imageOperation The operation to be performed over the {@link #actualImage}.
     * @param operationName  Operation name (to be used for logging).
     */
    private void oneImageOperationAction(Function<Image, Image> imageOperation, String operationName) {
        LOGGER.debug("Performing the {}...", operationName);
        final Image newImage = imageOperation.apply(this.actualImage);
        afterChanging(newImage);
    }


    /**
     * Performs the given {@code imageOperation},
     * using the {@link #actualImage} as first {@link Image},
     * and opening a new {@link Image} using the {@link #openImage()} method.
     * The result of the operation will be set a the new {@link #actualImage}.
     *
     * @param imageOperation The two {@link Image} operation to be performed.
     * @param operationName  Operation name (to be used for logging).
     */
    private void twoImagesOperationAction(BiFunction<Image, Image, Image> imageOperation, String operationName) {
        Optional.ofNullable(selectFile())
                .map(anotherImageFile -> {
                    try {
                        return imageFileService.openImage(anotherImageFile);
                    } catch (IOException e) {
                        LOGGER.error("Could not open image file.");
                        LOGGER.debug("Error message: {}", e.getMessage());
                        LOGGER.trace("Stacktrace: ", e);
                        return null;
                    }
                })
                .map(anotherImage -> imageOperation.apply(this.actualImage, anotherImage))
                .ifPresent(newImage -> {
                    LOGGER.debug("Performing {}...", operationName);
                    afterChanging(newImage);
                });
    }

    /**
     * Performs the last steps that must be done after changing the {@link #actualImage}.
     *
     * @param newImage The new {@link Image}.
     */
    private void afterChanging(Image newImage) {
        modify(newImage);
        drawActual();
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
     * Draws the actual image in the {@link #imageView} {@link ImageView}.
     */
    private void drawActual() {
        drawImage(this.actualImage.getContent(), this.imageView);
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
