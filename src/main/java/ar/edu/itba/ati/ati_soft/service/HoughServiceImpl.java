package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.HoughService;
import ar.edu.itba.ati.ati_soft.interfaces.SlidingWindowService;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link HoughService}.
 */
@Service
public class HoughServiceImpl implements HoughService {

    /**
     * A {@link SlidingWindowService} used to detect borders in the {@link Image} to be analyzed.
     */
    private final SlidingWindowService slidingWindowService;

    @Autowired
    public HoughServiceImpl(SlidingWindowService slidingWindowService) {
        this.slidingWindowService = slidingWindowService;
    }

    @Override
    public Image findStraightLines(Image image, double sigma, double thetaStep, double epsilon, double maxPercentage) {
        final Set<Shape> shapes = new HashSet<>();
        final int d = Math.max(image.getWidth(), image.getHeight());
        final double squaredRootOfTwo = Math.sqrt(2); // Avoid recalculating this
        for (double theta = -90d; theta <= 90d; theta += thetaStep) {
            for (double ro = -d * squaredRootOfTwo; ro <= d * squaredRootOfTwo; ro += squaredRootOfTwo) {
                shapes.add(new StraightLine(theta, ro, epsilon));
            }
        }
        return findShape(image, sigma, () -> shapes, maxPercentage);
    }

    @Override
    public Image findCircles(Image image, double sigma, double epsilon, double maxPercentage) {
        final Set<Shape> shapes = new HashSet<>();
        final int d = Math.max(image.getWidth(), image.getHeight());
        for (int radius = 1; radius <= d / 2; radius += 1) {
            for (int x = radius; x <= image.getWidth() - radius; x += 1) {
                for (int y = radius; y <= image.getHeight() - radius; y += 1) {
                    shapes.add(new Circle(x, y, radius, epsilon));
                }
            }
        }
        return findShape(image, sigma, () -> shapes, maxPercentage);
    }

    /**
     * Creates an black and white {@link Image}, setting those pixels that belongs to the {@link Shape}s the given
     * {@code shapeSetSupplier} supplies.
     *
     * @param image            The {@link Image} to be analyzed.
     * @param sigma            A sigma value used to detect borders before.
     * @param shapeSetSupplier A {@link Supplier} of {@link Set} of {@link Shape} that will inject the {@link Shape}s
     *                         that exists in the {@link Image}.
     * @param maxPercentage    A max. percentage, used to threshold the count.
     * @param <T>              The concrete type of {@link Shape} (e.g {@link StraightLine}).
     * @return The shapes {@link Image}.
     */
    private <T extends Shape> Image findShape(Image image, double sigma,
                                              Supplier<Set<T>> shapeSetSupplier, double maxPercentage) {
        // Canny already applies a threshold method
        final Image bordersImage = slidingWindowService.cannyDetection(image, sigma);
        final int width = bordersImage.getWidth();
        final int height = bordersImage.getHeight();

        final Set<T> shapes = shapeSetSupplier.get();
        final Map<T, Integer> shapesAccumulator = shapes.stream()
                .collect(Collectors.toMap(Function.identity(), shape -> 0));

        int max = 0; // Used in order to select only those that are greater than a given value
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (bordersImage.getSample(x, y, 0) == 255d) {
                    for (T shape : shapes) {
                        if (shape.belongs(x, y)) {
                            // Store the count for this shape
                            final int newCount = 1 + Optional.ofNullable(shapesAccumulator.get(shape))
                                    .orElseThrow(() -> new RuntimeException("This should not happen"));
                            shapesAccumulator.put(shape, newCount);
                            max = newCount > max ? newCount : max;
                        }
                    }
                }
            }
        }
        final double threshold = maxPercentage * max;
        final Set<Shape> allowedShapes = shapes.stream()
                .filter(shape -> Optional.ofNullable(shapesAccumulator.get(shape))
                        .orElseThrow(() -> new RuntimeException("This should not happen")) >= threshold)
                .collect(Collectors.toSet());

        return ImageManipulationHelper.createApplying(() -> Image.empty(width, height, 3),
                (x, y) -> {
                    if (allowedShapes.stream().anyMatch(shape -> shape.belongs(x, y))) {
                        return new Double[]{0d, 255d, 0d};
                    }
                    final Double[] pixel = image.getPixel(x, y);
                    if (image.getBands() == 3) {
                        return pixel;
                    }
                    final double gray = Math.sqrt(Arrays.stream(pixel).mapToDouble(v -> v * v).sum());
                    return new Double[]{gray, gray, gray};
                });
    }


    /**
     * Defines behaviour for and object that can represent a given shape (e.g a line or a circle).
     */
    private interface Shape {

        /**
         * Indicates whether a pixel in a given {@link Image}'s row {@code x} and column {@code y} belongs to the shape.
         *
         * @param x The {@link Image}'s row.
         * @param y The {@link Image}'s column.
         * @return {@code true} if it belongs, or {@code false} otherwise.
         */
        boolean belongs(int x, int y);
    }

    /**
     * A straight line {@link Shape}.
     */
    private final static class StraightLine implements Shape {

        /**
         * The angle formed between the 'x' axis and the normal line that exists between this line and the origin.
         */
        private final double theta;

        /**
         * The cosine of theta.
         */
        private final double cosTheta;

        /**
         * The sine of theta.
         */
        private final double sinTheta;

        /**
         * The length of the said line that goes from the origin to this line.
         */
        private final double ro;

        /**
         * An epsilon used to identify if a given pixel belongs to this line.
         */
        private final double epsilon;

        /**
         * Constructor.
         *
         * @param theta   The angle formed between the 'x' axis
         *                and the normal line that exists between this line and the origin.
         * @param ro      The length of the said line that goes from the origin to this line.
         * @param epsilon An epsilon used to identify if a given pixel belongs to this line.
         */
        private StraightLine(double theta, double ro, double epsilon) {
            this.theta = Math.toRadians(theta);
            this.cosTheta = Math.cos(this.theta);
            this.sinTheta = Math.sin(this.theta);
            this.ro = ro;
            this.epsilon = epsilon;
        }

        @Override
        public boolean belongs(int x, int y) {
            return Math.abs(ro - (double) x * sinTheta - (double) y * cosTheta) < epsilon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StraightLine)) {
                return false;
            }

            final StraightLine that = (StraightLine) o;
            return Double.compare(that.theta, theta) == 0 && Double.compare(that.ro, ro) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(theta);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(ro);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    private final static class Circle implements Shape {

        /**
         * The 'x' component of the center of this circle.
         */
        private final double xCenter;

        /**
         * The 'y' component of the center of this circle.
         */
        private final double yCenter;

        /**
         * The radius of this circle.
         */
        private final double radius;

        /**
         * The square of the radius (saved to avoid recalculating it).
         */
        private final double squaredRadius;

        /**
         * An epsilon used to identify if a given pixel belongs to this circle.
         */
        private final double epsilon;

        /**
         * Constructor.
         *
         * @param xCenter The 'x' component of the center of this circle.
         * @param yCenter The 'y' component of the center of this circle.
         * @param radius  The radius of this circle.
         * @param epsilon An epsilon used to identify if a given pixel belongs to this circle.
         */
        private Circle(double xCenter, double yCenter, double radius, double epsilon) {
            this.xCenter = xCenter;
            this.yCenter = yCenter;
            this.radius = radius;
            this.epsilon = epsilon;
            this.squaredRadius = radius * radius;
        }

        @Override
        public boolean belongs(int x, int y) {
            return Math.abs(squaredRadius - Math.pow(x - xCenter, 2) - Math.pow(y - yCenter, 2)) < epsilon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Circle)) {
                return false;
            }

            final Circle circle = (Circle) o;
            return Double.compare(circle.xCenter, xCenter) == 0
                    && Double.compare(circle.yCenter, yCenter) == 0
                    && Double.compare(circle.radius, radius) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(xCenter);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(yCenter);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(radius);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }
}
