package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.*;
import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.ToSeriesCollector;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * An {@link ImageThresholdService} used to create binary images.
     */
    private final ImageThresholdService imageThresholdService;

    /**
     * A {@link NoiseGenerationService} used to pollute images.
     */
    private final NoiseGenerationService noiseGenerationService;

    /**
     * A {@link SlidingWindowService} used to apply filters to images.
     */
    private final SlidingWindowService slidingWindowService;

    /**
     * A {@link HistogramService} used to calculate image histograms.
     */
    private final HistogramService histogramService;

    /**
     * A {@link DiffusionService} to perform image filtering using diffusion.
     */
    private final DiffusionService diffusionService;

    /**
     * A {@link HoughService} to perform shape detection.
     */
    private final HoughService houghService;


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
    private ImageMapping actual;

    /**
     * The last saved {@link Image}.
     */
    private ImageMapping lastSaved;

    /**
     * A {@link Stack} holding {@link Image}s obtained.
     */
    private final Stack<ImageMapping> imageHistory;

    /**
     * {@link Stack} holding each undone image (i.e those that were undone).
     */
    private final Stack<ImageMapping> undoneImages;


    // ==============================================================================
    // Constructor & Initialization
    // ==============================================================================

    @Autowired
    public HomeController(ImageIOService imageIOService,
                          ImageOperationService imageOperationService,
                          ImageThresholdService imageThresholdService,
                          NoiseGenerationService noiseGenerationService,
                          SlidingWindowService slidingWindowService,
                          HistogramService histogramService,
                          DiffusionService diffusionService,
                          HoughService houghService) {
        this.imageIOService = imageIOService;
        this.imageOperationService = imageOperationService;
        this.imageThresholdService = imageThresholdService;
        this.noiseGenerationService = noiseGenerationService;
        this.slidingWindowService = slidingWindowService;
        this.histogramService = histogramService;
        this.diffusionService = diffusionService;
        this.houghService = houghService;
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

    @FXML
    public void newHomogeneousImage() {
        getNumber("Homogeneous image creation", "Insert the image width",
                "Insert the image width", Integer::parseInt)
                .ifPresent(width -> getNumber("Homogeneous image creation", "Insert the image height",
                        "Insert the image height", Integer::parseInt)
                        .ifPresent(height ->
                                getNumber("Homogeneous image creation",
                                        "Insert the amount of bands (1 for gray, 3 for RGB)",
                                        "Insert the amount of bands", Integer::parseInt)
                                        .ifPresent(bands -> getNumber("Homogeneous image creation",
                                                "Insert the pixels intensity",
                                                "Insert the pixels intensity", Integer::parseInt)
                                                .ifPresent(value -> {
                                                    final Image image = Image.homogeneous(width, height, bands, value);
                                                    final BufferedImage buffered = imageIOService
                                                            .toImageIO(ImageIOContainer.buildForSyntheticImage(image));
                                                    setUp(buffered, null);
                                                }))));
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
        Optional.ofNullable(selectFile()).ifPresent(this::openImage);
    }

    @FXML
    public void saveImage() {
        LOGGER.debug("Saving image...");
        validateSave();
        final File file = Optional.ofNullable(this.openedImageFile)
                .orElseGet(() -> {
                    // First save...
                    final FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save...");
                    fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                    final int bands = actual.getInternalRepresentation().getBands();
                    final String name = bands == 1 ? ".pgm" : bands == 3 ? ".ppm" : "";
                    fileChooser.setInitialFileName(name);
                    return fileChooser.showSaveDialog(root.getScene().getWindow());
                });
        // If after the optional stuff the file continues to be null, then finish
        if (file == null) {
            return;
        }
        if (actual.getImageIORepresentation() == lastSaved.getImageIORepresentation()) {
            LOGGER.warn("No changes has been made to image.");
        }
        saveImage(file);
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
    public void normalize() {
        oneImageOperationAction(imageOperationService::normalize, "normalization", Function.identity());
    }

    @FXML
    public void negative() {
        oneImageOperationAction(imageOperationService::getNegative, "negative calculation", Function.identity());
    }

    @FXML
    public void threshold() {
        getNumber("Threshold value", "", "Insert the threshold", Integer::parseInt)
                .ifPresent(value -> oneImageOperationAction(image -> imageThresholdService.manualThreshold(image, value),
                        "manual threshold function", imageOperationService::normalize));
    }

    @FXML
    public void globalThreshold() {
        getNumber("Delta T value for Global Threshold", "", "Insert the delta T", Integer::parseInt)
                .ifPresent(deltaT ->
                        oneImageOperationAction(image -> imageThresholdService.globalThreshold(image, deltaT),
                                "global threshold", Function.identity()));
    }

    @FXML
    public void otsuThreshold() {
        oneImageOperationAction(imageThresholdService::otsuThreshold, "Otsu threshold", Function.identity());
    }

    @FXML
    public void hysteresisThreshold() {
        oneImageOperationAction(imageThresholdService::hysteresisThreshold,
                "Hysteresis threshold", Function.identity());
    }

    @FXML
    public void equalize() {
        oneImageOperationAction(histogramService::equalize, "image equalization",
                imageOperationService::normalize);
    }

    @FXML
    public void increaseContrast() {
        oneImageOperationAction(histogramService::increaseContrast, "contrast increase",
                imageOperationService::normalize);
    }

    @FXML
    public void additiveGaussianNoise() {
        getNumber("Mean value for Additive Gaussian Noise", "", "Insert the mean value", Double::parseDouble)
                .ifPresent(mean -> getNumber("Standard Deviation value for Additive Gaussian Noise", "",
                        "Insert the standard deviation value", Double::parseDouble)
                        .ifPresent(stdDev ->
                                getNumber("Noise density", "Insert the noise density between 0.0 and 1.0",
                                        "Insert the density", Double::parseDouble)
                                        .ifPresent(density ->
                                                oneImageOperationAction(image -> noiseGenerationService
                                                                .additiveGaussianNoise(image, mean, stdDev, density),
                                                        "addition of Additive Gaussian Noise",
                                                        imageOperationService::normalize))));
    }

    @FXML
    public void multiplicativeRayleighNoise() {
        getNumber("Scale value for Multiplicative Rayleigh Nose", "",
                "Insert the scale value (i.e the xi value)", Double::parseDouble)
                .ifPresent(scale ->
                        getNumber("Noise density", "Insert the noise density between 0.0 and 1.0",
                                "Insert the density", Double::parseDouble)
                                .ifPresent(density ->
                                        oneImageOperationAction(image -> noiseGenerationService
                                                        .multiplicativeRayleighNoise(image, scale, density),
                                                "addition of Multiplicative Rayleigh Noise",
                                                imageOperationService::normalize)));
    }

    @FXML
    public void multiplicativeExponentialNoise() {
        getNumber("Rate value for Multiplicative Exponential Nose", "",
                "Insert the rate value (i.e the lambda value)", Double::parseDouble)
                .ifPresent(rate ->
                        getNumber("Noise density", "Insert the noise density between 0.0 and 1.0",
                                "Insert the density", Double::parseDouble)
                                .ifPresent(density ->
                                        oneImageOperationAction(image -> noiseGenerationService
                                                        .multiplicativeExponentialNoise(image, rate, density),
                                                "addition of Multiplicative Exponential Noise",
                                                imageOperationService::normalize)));
    }

    @FXML
    public void saltAndPepperNoise() {
        getNumber("Lower limit value (p0) for Salt and Pepper Noise", "",
                "Insert the p0 value", Double::parseDouble)
                .ifPresent(p0 -> getNumber("Upper limit value (p1) for Salt and Pepper Noise", "",
                        "Insert the p1 value", Double::parseDouble)
                        .ifPresent(p1 -> oneImageOperationAction(image ->
                                        noiseGenerationService.saltAndPepperNoise(image, p0, p1),
                                "addition of Salt and Pepper Noise", imageOperationService::normalize)));
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

    @FXML
    public void weightedMedianFilter() {
        getNumberArray("Weights mask for weighted median filter",
                "Insert mask by separating values with commas. Every three values a row will be created. " +
                        "Insert just 9 values", "Insert the weight mask", Integer::parseInt, 9)
                .map(list -> ListUtils.partition(list, 3))
                .map(lists ->
                        lists.stream().map(list -> list.toArray(new Integer[list.size()])).toArray(Integer[][]::new))
                .ifPresent(filter ->
                        oneImageOperationAction(image -> slidingWindowService.applyWeightMedianFilter(image, filter),
                                "Weighted Median Filtering", imageOperationService::normalize));
    }

    @FXML
    public void gaussianFilter() {
        getNumber("Standard deviation for Gaussian Filter", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(standardDeviation -> oneImageOperationAction(image ->
                                slidingWindowService.applyGaussianFilter(image, standardDeviation),
                        "Gaussian Filtering", imageOperationService::normalize));
    }

    @FXML
    public void highPassFilter() {
        getNumber("Window length for High-Pass Filter", "",
                "Insert the window's length", Integer::parseInt)
                .ifPresent(length -> oneImageOperationAction(image ->
                                slidingWindowService.applyHighPassFilter(image, length),
                        "High-Pass Filtering", imageOperationService::normalize));
    }

    @FXML
    public void borderDetectionWithPrewittGradientOperator() {
        oneImageOperationAction(slidingWindowService::prewittGradientOperatorBorderDetectionMethod,
                "border detection with Prewitt's gradient operator", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithSobelGradientOperator() {
        oneImageOperationAction(slidingWindowService::sobelGradientOperatorBorderDetectionMethod,
                "border detection with Sobel's gradient operator", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithAnonymousMaxDirection() {
        oneImageOperationAction(slidingWindowService::anonymousMaxDirectionBorderDetectionMethod,
                "border detection with anonymous's max direction", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithKirshMaxDirection() {
        oneImageOperationAction(slidingWindowService::kirshMaxDirectionBorderDetectionMethod,
                "border detection with Kirsh's max direction", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithPrewittMaxDirection() {
        oneImageOperationAction(slidingWindowService::prewittMaxDirectionBorderDetectionMethod,
                "border detection with Prewitt's max direction", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithSobelMaxDirection() {
        oneImageOperationAction(slidingWindowService::sobelMaxDirectionBorderDetectionMethod,
                "border detection with Sobel's max direction", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithLaplaceMethod() {
        oneImageOperationAction(slidingWindowService::laplaceMethod,
                "border detection with Laplace's", imageOperationService::normalize);
    }

    @FXML
    public void borderDetectionWithLaplaceMethodAndSlopeEvaluation() {
        getNumber("Slope threshold for Laplace method", "", "Insert the threshold", Double::parseDouble)
                .ifPresent(threshold -> oneImageOperationAction(image ->
                                slidingWindowService.laplaceMethodWithSlopeEvaluation(image, threshold),
                        "border detection with Laplace's", imageOperationService::normalize));
    }

    @FXML
    public void borderDetectionWithLaplaceOfGaussianMethod() {
        getNumber("Standard deviation for Laplace of Gaussian", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma -> oneImageOperationAction(image ->
                                slidingWindowService.laplaceOfGaussianMethod(image, sigma),
                        "border detection with Laplace's", imageOperationService::normalize));
    }

    @FXML
    public void borderDetectionWithLaplaceOfGaussianMethodAndSlopeEvaluation() {
        getNumber("Standard deviation for Laplace of Gaussian", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma -> getNumber("Slope threshold for Laplace method", "",
                        "Insert the threshold", Double::parseDouble)
                        .ifPresent(threshold -> oneImageOperationAction(image -> slidingWindowService
                                        .laplaceOfGaussianWithSlopeEvaluation(image, sigma, threshold),
                                "border detection with Laplace's", imageOperationService::normalize)));
    }

    @FXML
    public void suppressNoMax() {
        getNumber("Standard deviation for gaussian filtering for the No max suppression", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma -> oneImageOperationAction(image ->
                                slidingWindowService.suppressNoMaxPixels(image, sigma),
                        "border detection with Canny method", imageOperationService::normalize));
    }

    @FXML
    public void cannyDetector() {
        getNumber("Standard deviation for gaussian filtering for the Canny Border detection method", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma -> oneImageOperationAction(image -> slidingWindowService.cannyDetection(image, sigma),
                        "border detection with Canny method", Function.identity()));
    }

    @FXML
    public void susanDetector() {
        getNumber("T value for the susan detection method", "",
                "Insert the t value", Double::parseDouble)
                .ifPresent(t -> {
                    LOGGER.debug("Performing the border detection with SUSAN method...");
                    final Image newImage = slidingWindowService.susanDetection(this.actual.getInternalRepresentation(), t);
                    afterChanging(newImage, Function.identity(), ImageIOContainer::buildForNewColorImage);
                });
    }

    @FXML
    public void isotropicDiffusion() {
        getNumber("Amount of iterations for Isotropic Diffusion", "",
                "Insert the amount of iterations", Integer::parseInt)
                .ifPresent(t -> getNumber("Lambda value for the discrete equation", "",
                        "Insert the lambda value", Double::parseDouble)
                        .ifPresent(lambda -> oneImageOperationAction(image -> diffusionService
                                        .isotropicDiffusion(image, t, lambda),
                                "isotropic diffusion", imageOperationService::normalize)));
    }

    @FXML
    public void leclercAnisotropicDiffusion() {
        getNumber("Amount of iterations for Anisotropic Diffusion", "",
                "Insert the amount of iterations", Integer::parseInt)
                .ifPresent(t -> getNumber("Lambda value for the discrete equation", "",
                        "Insert the lambda value", Double::parseDouble)
                        .ifPresent(lambda -> getNumber("Sigma value for Leclerc detector", "",
                                "Insert the sigma value", Double::parseDouble)
                                .ifPresent(sigma -> oneImageOperationAction(image -> diffusionService
                                                .anisotropicDiffusionWithLeclerc(image, t, lambda, sigma),
                                        "Leclerc anisotropic diffusion", imageOperationService::normalize))));
    }

    @FXML
    public void lorentzAnisotropicDiffusion() {
        getNumber("Amount of iterations for Anisotropic Diffusion", "",
                "Insert the amount of iterations", Integer::parseInt)
                .ifPresent(t -> getNumber("Lambda value for the discrete equation", "",
                        "Insert the lambda value", Double::parseDouble)
                        .ifPresent(lambda -> getNumber("Sigma value for Lorentz detector", "",
                                "Insert the sigma value", Double::parseDouble)
                                .ifPresent(sigma -> oneImageOperationAction(image -> diffusionService
                                                .anisotropicDiffusionWithLorentz(image, t, lambda, sigma),
                                        "Lorentz anisotropic diffusion", imageOperationService::normalize))));
    }

    @FXML
    public void detectStraightLines() {
        getNumber("Standard deviation for gaussian filtering for the Canny Border detection method", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma ->
                        getNumber("Theta step for the Hough accumulator matrix", "",
                                "Insert the theta step", Double::parseDouble)
                                .ifPresent(thetaStep -> getNumber("Epsilon for the Straight Line detector", "",
                                        "Insert the epsilon", Double::parseDouble)
                                        .ifPresent(epsilon -> getNumber("Max percentage", "",
                                                "Insert the percentage of the max to be taken into account",
                                                Double::parseDouble)
                                                .ifPresent(maxPercentage ->
                                                        oneImageOperationAction(image ->
                                                                        houghService.findStraightLines(image,
                                                                                sigma,
                                                                                thetaStep,
                                                                                epsilon,
                                                                                maxPercentage),
                                                                "Hough transform for straight lines",
                                                                Function.identity())))));
    }

    @FXML
    public void detectCircles() {
        getNumber("Standard deviation for gaussian filtering for the Canny Border detection method", "",
                "Insert the standard deviation", Double::parseDouble)
                .ifPresent(sigma -> getNumber("Epsilon for the Straight Line detector", "",
                        "Insert the epsilon", Double::parseDouble)
                        .ifPresent(epsilon -> getNumber("Max percentage", "",
                                "Insert the percentage of the max to be taken into account",
                                Double::parseDouble)
                                .ifPresent(maxPercentage ->
                                        oneImageOperationAction(image ->
                                                        houghService.findCircles(image,
                                                                sigma,
                                                                epsilon,
                                                                maxPercentage),
                                                "Hough transform for circles",
                                                Function.identity()))));
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

    @FXML
    public void showHistograms() {
        histogramService.getHistograms(imageOperationService.normalize(this.actual.getInternalRepresentation()))
                .forEach((b, h) -> showHistogram(h, "Histogram for band " + b));
    }

    @FXML
    public void showCumulativeDistributionHistograms() {
        histogramService.getHistograms(imageOperationService.normalize(this.actual.getInternalRepresentation()))
                .forEach((b, h) ->
                        showHistogram(histogramService.getCumulativeDistributionHistogram(h),
                                "Cumulative Distribution Histogram for band " + b));
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
            setUp(image, imageFile);
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
    private void setUp(BufferedImage image, File file) {
        this.initialDisplayed = image;
        final ImageIOContainer initialImageIOContainer = imageIOService.fromImageIO(image);
        this.actual = new ImageMapping(initialImageIOContainer, image);
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
            imageIOService.saveImage(this.actual.getImageIORepresentation(), file);
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
//        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
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
     * Show ups a {@link javafx.scene.control.Dialog} that expects a list of numeric value to be inserted.
     * The transformation is performed using a {@link Function} that takes a {@link String}
     * and converts it into a {@link Number}.
     * In case the inserted value is not a number that can be created with the {@code converter},
     * an {@link javafx.scene.control.Alert.AlertType#ERROR} {@link Alert} is showed up.
     * Also, if the amount of elements inserted is not the {@code requiredAmount}, an error message is displayed.
     *
     * @param header         The message to be displayed in the {@link javafx.scene.control.Dialog} header.
     * @param defaultValue   The default value for the {@link javafx.scene.control.Dialog}
     *                       (i.e what appears in the text field).
     * @param converter      A {@link Function} to be used to create the {@link Number}.
     * @param requiredAmount The required amount of elements that must be inserted.
     * @param <N>            The concrete subtype of {@link Number} (e.g {@link Integer} or {@link Double}).
     * @return An {@link Optional} of {@code N} holding the inserted value,
     * or empty if no value, no number was inserted, or not the {@code requiredAmount} of values were inserted.
     */
    private <N extends Number> Optional<List<N>> getNumberArray(String title, String header, String defaultValue,
                                                                Function<String, N> converter, int requiredAmount) {
        final TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setHeaderText(header);
        textInputDialog.setTitle(title);
        final List<N> values = textInputDialog.showAndWait()
                .map(str -> str.split(",", -1))
                .map(Arrays::stream)
                .map(stream -> stream.map(value -> convertToNumber(value, converter)).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        if (values.size() != requiredAmount) {
            final Alert alert = new Alert(Alert.AlertType.ERROR,
                    "The amount of inserted values is not the required amount. "
                            + requiredAmount + " values must be inserted");
            alert.setHeaderText("");
            alert.show();
            return Optional.empty();
        }
        return Optional.of(values);
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
        final Image newImage = imageOperation.apply(this.actual.getInternalRepresentation());
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
                .map(anotherImage -> imageOperation.apply(this.actual.getInternalRepresentation(), anotherImage))
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
     *                         (e.g normalization, dynamic range compression, etc.).
     * @implNote This method calls the {@link #afterChanging(Image, Function, Function)} method,
     * but delegating the task of building a new {@link ImageIOContainer} to the actual one.
     */
    private void afterChanging(Image newImage, Function<Image, Image> displayOperation) {
        afterChanging(newImage, displayOperation, this.actual.getImageIOContainer()::buildForNewImage);
    }

    /**
     * Performs the last steps that must be done after changing the actual image.
     *
     * @param newImage          The new {@link Image}.
     * @param displayOperation  A {@link Function} that takes the new {@link Image}
     *                          and performs the operation that must be done to be displayed.
     *                          (e.g normalization, dynamic range compression, etc.)
     * @param toImageIOFunction A {@link Function} that takes an {@link Image}
     *                          and transforms it into an {@link ImageIOContainer} for it.
     */
    private void afterChanging(Image newImage,
                               Function<Image, Image> displayOperation,
                               Function<Image, ImageIOContainer> toImageIOFunction) {
        modify(new ImageMapping(newImage, displayOperation, toImageIOFunction, imageIOService::toImageIO));
        drawActual();
    }

    /**
     * Modifies the actual image, setting the given {@link ImageMapping} as the actual,
     * saving the ex-actual in the {@link #imageHistory} {@link Stack},
     * and clearing the {@link #undoneImages} {@link Stack}.
     *
     * @param newPair The new actual image (i.e the original-display pair).
     */
    private void modify(ImageMapping newPair) {
        this.imageHistory.push(this.actual);
        this.undoneImages.clear();
        this.actual = newPair;
    }

    /**
     * Draws the actual image in the {@link #imageView} {@link ImageView}.
     */
    private void drawActual() {
        drawImage(this.actual.getImageIORepresentation(), this.imageView);
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


    /**
     * Shows the given histogram, displaying the given {@code seriesName}.
     *
     * @param histogram  The {@link Histogram} to be displayed.
     * @param seriesName The name for the series in the displayed chart.
     */
    private static void showHistogram(Histogram histogram, String seriesName) {
        // Generate chart data
        final int min = histogram.minCategory();
        final int max = histogram.maxCategory();
        final XYChart.Series<String, Number> series = IntStream.range(min, max + 1)
                .mapToObj(i -> new XYChart.Data<String, Number>(Integer.toString(i), histogram.getFrequency(i)))
                .collect(new ToSeriesCollector<>());
        series.setName(seriesName);
        // Generate chart
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setCategoryGap(0);
        barChart.setBarGap(0);
        xAxis.setLabel("Gray level");
        yAxis.setLabel("Relative Frequency");
        barChart.getData().addAll(Collections.singleton(series));
        // Create window
        final VBox vBox = new VBox();
        vBox.getChildren().addAll(barChart);
        StackPane root = new StackPane();
        root.getChildren().add(vBox);
        Scene scene = new Scene(root, 800, 450);
        final Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
    }

    // ==============================================================================
    // Helper classes
    // ==============================================================================

    /**
     * Bean class that holds together an {@link Image} with its {@link BufferedImage} representation,
     * and an {@link ImageIOContainer} also, which is used to perform the mapping.
     */
    private static final class ImageMapping {

        /**
         * The {@link BufferedImage} that will be displayed.
         */
        private final BufferedImage displayed;

        /**
         * The {@link ImageIOContainer} that was used to perform the {@link Image} - {@link BufferedImage} mapping.
         */
        private final ImageIOContainer imageIOContainer;

        /**
         * Constructor that builds the displayed {@link BufferedImage}.
         *
         * @param original                The original {@link Image} (i.e the model).
         * @param displayOperation        A {@link Function} that takes the {@code original}
         *                                and transform it into another {@link Image} that can be displayed.
         * @param toImageIOContainer      A {@link Function} that takes an {@link Image}
         *                                and produces an {@link ImageIOContainer} for it.
         * @param toBufferedImageFunction A {@link Function} that takes an {@link ImageIOContainer}
         *                                and transforms it into the {@link BufferedImage}.
         * @apiNote This constructor can be used when, having an internal {@link Image},
         * one wants to hold the mapping for it into an ImageIO {@link BufferedImage}.
         */
        private ImageMapping(Image original, Function<Image, Image> displayOperation,
                             Function<Image, ImageIOContainer> toImageIOContainer,
                             Function<ImageIOContainer, BufferedImage> toBufferedImageFunction) {
            this.imageIOContainer = toImageIOContainer.apply(displayOperation.apply(original));
            this.displayed = toBufferedImageFunction.apply(this.imageIOContainer);
        }

        /**
         * Constructor that sets values.
         *
         * @param imageIOContainer The {@link ImageIOContainer}
         *                         that was used to perform the {@link Image} - {@link BufferedImage} mapping.
         * @param displayed        The {@link BufferedImage}
         *                         (i.e the displayed representation of the {@code original}) {@link Image}.
         * @apiNote This constructor can be used when, having the {@link BufferedImage},
         * one wants to hold the mapping for it into an internal {@link Image}.
         */
        private ImageMapping(ImageIOContainer imageIOContainer, BufferedImage displayed) {
            this.imageIOContainer = imageIOContainer;
            this.displayed = displayed;
        }

        /**
         * @return The original {@link Image} (i.e the model).
         */
        private Image getInternalRepresentation() {
            return imageIOContainer.getImage();
        }


        /**
         * @return The {@link ImageIOContainer}
         * that was used to perform the {@link Image} - {@link BufferedImage} mapping.
         */
        private ImageIOContainer getImageIOContainer() {
            return imageIOContainer;
        }

        /**
         * @return The {@link BufferedImage}
         * (i.e the displayed representation of the {@link #getInternalRepresentation()} {@link Image}).
         */
        private BufferedImage getImageIORepresentation() {
            return displayed;
        }
    }
}
