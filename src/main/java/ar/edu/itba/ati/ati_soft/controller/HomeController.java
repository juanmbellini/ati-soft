package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.*;
import ar.edu.itba.ati.ati_soft.models.Image;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
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
     * An {@link ImageIOService} to perform ImageIO stuff (opening, saving, translating, etc).
     */
    private final ImageIOService imageIOService;

    /**
     * An {@link ImageOperationService} to perform image operations over the actual image.
     */
    private final ImageOperationService imageOperationService;

    /**
     * A {@link NoiseGenerationService} used to pollute images.
     */
    private final NoiseGenerationService noiseGenerationService;

    /**
     * A {@link SlidingWindowService} used to apply filters to images.
     */
    private final SlidingWindowService slidingWindowService;


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
    // State
    // ==============================================================================

    // ================================
    // Initial stuff
    // ================================

    /**
     * The {@link File} from where the image was opened (i.e used for saving the image).
     */
    private File openedImageFile;

    /**
     * An {@link ImageIOContainer} that holds the opened {@link Image}.
     * This is used to build a new {@link BufferedImage} each time the actual {@link Image} is changed
     * (i.e just the metadata is used).
     */
    private ImageIOContainer initialImageIOContainer;

    /**
     * The initially displayed {@link BufferedImage},
     * used for quickly display of the original image with the {@link #showOriginal()} method.
     */
    private BufferedImage initialDisplayed;


    // ================================
    // Actual stuff
    // ================================

    /**
     * The actual image being displayed.
     */
    private ImagePair actual;

    /**
     * The last saved {@link Image}.
     */
    private ImagePair lastSaved;

    /**
     * A {@link Stack} holding {@link Image}s obtained.
     */
    private final Stack<ImagePair> imageHistory;

    /**
     * {@link Stack} holding each undone image (i.e those that were undone).
     */
    private final Stack<ImagePair> undoneImages;


    // ==============================================================================
    // Constructor & Initialization
    // ==============================================================================

    @Autowired
    public HomeController(ImageIOService imageIOService,
                          ImageOperationService imageOperationService,
                          NoiseGenerationService noiseGenerationService,
                          SlidingWindowService slidingWindowService) {
        this.imageIOService = imageIOService;
        this.imageOperationService = imageOperationService;
        this.noiseGenerationService = noiseGenerationService;
        this.slidingWindowService = slidingWindowService;
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
        Optional.ofNullable(selectFile()).ifPresent(this::openImage);
    }

    @FXML
    public void saveImage() {
        LOGGER.debug("Saving image...");
        validateSave();
        if (actual.getDisplayed() == lastSaved.getDisplayed()) {
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
        twoImagesOperationAction(imageOperationService::sum, "sum", imageOperationService::normalize);
    }

    @FXML
    public void subtract() {
        twoImagesOperationAction(imageOperationService::subtract, "subtraction", imageOperationService::normalize);
    }

    @FXML
    public void multiply() {
        twoImagesOperationAction(imageOperationService::multiply, "multiplication", imageOperationService::normalize);
    }

    @FXML
    public void multiplyByScalar() {
        getNumber("Multiplication by scalar", "", "Insert the scalar", Double::parseDouble)
                .ifPresent(scalar ->
                        oneImageOperationAction(image -> imageOperationService.multiplyByScalar(image, scalar),
                                "scalar multiplication", imageOperationService::dynamicRangeCompression));
    }

    @FXML
    public void dynamicRangeCompression() {
        oneImageOperationAction(imageOperationService::dynamicRangeCompression, "dynamic range compression", Function.identity());
    }

    @FXML
    public void gammaPower() {
        getNumber("Gamma power", "", "Insert the gamma value", Double::parseDouble)
                .ifPresent(gamma ->
                        oneImageOperationAction(image -> imageOperationService.gammaPower(image, gamma),
                                "gamma power transformation", imageOperationService::normalize));
    }

    @FXML
    public void negative() {
        oneImageOperationAction(imageOperationService::getNegative, "negative calculation", Function.identity());
    }

    @FXML
    public void additiveGaussianNoise() {
        getNumber("Mean value for Additive Gaussian Noise", "", "Insert the mean value", Double::parseDouble)
                .ifPresent(mean -> getNumber("Standard Deviation value for Additive Gaussian Noise", "",
                        "Insert the standard deviation value", Double::parseDouble)
                        .ifPresent(stdDev -> oneImageOperationAction(image ->
                                        noiseGenerationService.additiveGaussianNoise(image, mean, stdDev),
                                "addition of Additive Gaussian Noise", imageOperationService::normalize)));
    }

    @FXML
    public void multiplicativeRayleighNoise() {
        getNumber("Scale value for Multiplicative Rayleigh Nose", "",
                "Insert the scale value (i.e the xi value)", Double::parseDouble)
                .ifPresent(scale -> oneImageOperationAction(image ->
                                noiseGenerationService.multiplicativeRayleighNoise(image, scale),
                        "addition of Multiplicative Rayleigh Noise", imageOperationService::normalize));
    }

    @FXML
    public void multiplicativeExponentialNoise() {
        getNumber("Rate value for Multiplicative Exponential Nose", "",
                "Insert the rate value (i.e the lambda value)", Double::parseDouble)
                .ifPresent(rate -> oneImageOperationAction(image ->
                                noiseGenerationService.multiplicativeExponentialNoise(image, rate),
                        "addition of Multiplicative Exponential Noise", imageOperationService::normalize));
    }

    @FXML
    public void meanFilter() {
        getNumber("Window length for Mean Filter", "",
                "Insert the window's length", Integer::parseInt)
                .ifPresent(length -> oneImageOperationAction(image ->
                                slidingWindowService.applyMeanFilter(image, length),
                        "Mean Filtering", imageOperationService::normalize));
    }

    @FXML
    public void medianFilter() {
        getNumber("Window length for Median Filter", "",
                "Insert the window's length", Integer::parseInt)
                .ifPresent(length -> oneImageOperationAction(image ->
                                slidingWindowService.applyMedianFilter(image, length),
                        "Median Filtering", imageOperationService::normalize));
    }

    // ======================================
    // View actions
    // ======================================

    @FXML
    public void showOriginal() {
        final ImageView originalImageView = new ImageView();
        drawImage(initialDisplayed, originalImageView);
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
     * Opens the given {@code imageFile}, performing the necessary steps to initialize everything.
     *
     * @param imageFile The {@link File} containing the image to be opened.
     */
    private void openImage(File imageFile) {
        try {
            final BufferedImage image = imageIOService.openImage(imageFile);
            afterOpeningImage(image, imageFile);
        } catch (UnsupportedImageFileException e) {
            LOGGER.debug("File is not an image");
        } catch (IOException e) {
            LOGGER.debug("Could not open image");
        }
    }

    /**
     * Performs the operations that must be done after an image is opened.
     *
     * @param image The opened {@link Image}.
     * @param file  The {@link File} from where the image was opened.
     */
    private void afterOpeningImage(BufferedImage image, File file) {
        this.initialDisplayed = image;
        this.initialImageIOContainer = imageIOService.fromImageIO(image);
        this.actual = new ImagePair(this.initialImageIOContainer.getImage(), image);
        this.lastSaved = this.actual;
        this.openedImageFile = file;
        this.imageHistory.clear();
        this.undoneImages.clear();
        drawActual();
    }

    /**
     * Validates that the save operation can be performed.
     *
     * @throws IllegalStateException If the state of the system does not allow to perform a save operation.
     */
    private void validateSave() throws IllegalStateException {
        if (this.actual == null) {
            throw new IllegalStateException("No opened image.");
        }
    }

    /**
     * Performs the save operation, using the given {@link File}.
     *
     * @param file {@link File} where the actual image will be saved.
     */
    private void saveImage(File file) {
        try {
            imageIOService.saveImage(this.actual.getDisplayed(), file);
        } catch (UnsupportedImageFileException e) {
            LOGGER.debug("Something wrong had happened.");
        } catch (IOException e) {
            LOGGER.debug("Could not save image.");
        }
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
        final List<FileChooser.ExtensionFilter> extensionFilters = imageIOService.getSupportedFormats().entrySet()
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
     * Performs the given {@code imageOperation}, applying it to the given actual image.
     * The result of the operation will be set as the new actual image.
     *
     * @param imageOperation   The operation to be performed over the actual image.
     * @param operationName    Operation name (to be used for logging).
     * @param displayOperation A {@link Function} that takes the new {@link Image}
     *                         and performs the operation that must be done to be displayed.
     *                         (e.g normalization, dynamic range compression, etc.)
     */
    private void oneImageOperationAction(Function<Image, Image> imageOperation, String operationName,
                                         Function<Image, Image> displayOperation) {
        LOGGER.debug("Performing the {}...", operationName);
        final Image newImage = imageOperation.apply(this.actual.getOriginal());
        afterChanging(newImage, displayOperation);
    }


    /**
     * Performs the given {@code imageOperation},
     * using the actual image as first {@link Image},
     * and opening a new {@link Image} using the {@link #openImage()} method.
     * The result of the operation will be set a the new actual image.
     *
     * @param imageOperation   The two {@link Image} operation to be performed.
     * @param operationName    Operation name (to be used for logging).
     * @param displayOperation A {@link Function} that takes the new {@link Image}
     *                         and performs the operation that must be done to be displayed.
     *                         (e.g normalization, dynamic range compression, etc.)
     */
    private void twoImagesOperationAction(BiFunction<Image, Image, Image> imageOperation, String operationName,
                                          Function<Image, Image> displayOperation) {
        Optional.ofNullable(selectFile())
                .map(anotherImageFile -> {
                    try {
                        return imageIOService.openImage(anotherImageFile);
                    } catch (IOException e) {
                        LOGGER.error("Could not open image file.");
                        LOGGER.debug("Error message: {}", e.getMessage());
                        LOGGER.trace("Stacktrace: ", e);
                        return null;
                    }
                })
                .map(imageIOService::fromImageIO)
                .map(ImageIOContainer::getImage)
                .map(anotherImage -> imageOperation.apply(this.actual.getOriginal(), anotherImage))
                .ifPresent(newImage -> {
                    LOGGER.debug("Performing {}...", operationName);
                    afterChanging(newImage, displayOperation);
                });
    }

    /**
     * Performs the last steps that must be done after changing the actual image.
     *
     * @param newImage         The new {@link Image}.
     * @param displayOperation A {@link Function} that takes the new {@link Image}
     *                         and performs the operation that must be done to be displayed.
     *                         (e.g normalization, dynamic range compression, etc.)
     */
    private void afterChanging(Image newImage, Function<Image, Image> displayOperation) {
        modify(new ImagePair(newImage, displayOperation,
                image -> imageIOService.toImageIO(this.initialImageIOContainer.buildForNewImage(image))));
        drawActual();
    }

    /**
     * Modifies the actual image, setting the given {@link ImagePair} as the actual,
     * saving the ex-actual in the {@link #imageHistory} {@link Stack},
     * and clearing the {@link #undoneImages} {@link Stack}.
     *
     * @param newPair The new actual image (i.e the original-display pair).
     */
    private void modify(ImagePair newPair) {
        this.imageHistory.push(this.actual);
        this.undoneImages.clear();
        this.actual = newPair;
    }

    /**
     * Draws the actual image in the {@link #imageView} {@link ImageView}.
     */
    private void drawActual() {
        drawImage(this.actual.getDisplayed(), this.imageView);
    }

    /**
     * Performs the "undo" operation.
     */
    private void doUndo() {
        if (imageHistory.isEmpty()) {
            return;
        }
        this.undoneImages.push(this.actual);
        this.actual = this.imageHistory.pop();
    }

    /**
     * Performs the "redo" operation.
     */
    private void doRedo() {
        if (undoneImages.isEmpty()) {
            return;
        }
        this.imageHistory.push(this.actual);
        this.actual = this.undoneImages.pop();
    }

    /**
     * Draws the given {@link BufferedImage} into the given {@link ImageView}.
     *
     * @param image     The image to be drawn.
     * @param imageView The {@link ImageView} to which the image will be drawn.
     */
    private void drawImage(BufferedImage image, ImageView imageView) {
        imageView.setImage(SwingFXUtils.toFXImage(image, null));
    }

    // ==============================================================================
    // Helper classes
    // ==============================================================================

    /**
     * Bean class that holds together an {@link Image} with its {@link BufferedImage} representation.
     */
    private static final class ImagePair {

        /**
         * The original {@link Image} (i.e the model).
         */
        private final Image original;

        /**
         * The {@link BufferedImage} (i.e the displayed representation of the {@link #original} {@link Image}).
         */
        private final BufferedImage displayed;

        /**
         * Constructor that builds the displayed {@link BufferedImage}.
         *
         * @param original                The original {@link Image} (i.e the model).
         * @param displayOperation        A {@link Function} that takes the {@code original}
         *                                and transform it into another {@link Image} that can be displayed.
         * @param toBufferedImageFunction A {@link Function} that takes an {@link Image}
         *                                and transforms it into the {@link BufferedImage} representation of it.
         */
        private ImagePair(Image original, Function<Image, Image> displayOperation,
                          Function<Image, BufferedImage> toBufferedImageFunction) {
            this.original = original;
            this.displayed = displayOperation.andThen(toBufferedImageFunction).apply(this.original);
        }

        /**
         * Constructor that sets values.
         *
         * @param original  The original {@link Image} (i.e the model).
         * @param displayed The {@link BufferedImage}
         *                  (i.e the displayed representation of the {@code original}) {@link Image}.
         */
        private ImagePair(Image original, BufferedImage displayed) {
            this.original = original;
            this.displayed = displayed;
        }

        /**
         * @return The original {@link Image} (i.e the model).
         */
        private Image getOriginal() {
            return original;
        }

        /**
         * @return The {@link BufferedImage}
         * (i.e the displayed representation of the {@link #getOriginal()} {@link Image}).
         */
        private BufferedImage getDisplayed() {
            return displayed;
        }
    }
}
