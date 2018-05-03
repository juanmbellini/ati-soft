package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageThresholdService;
import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Concrete implementation of {@link ImageThresholdService}.
 */
@Service
public class ImageThresholdServiceImpl implements ImageThresholdService {

    @Override
    public Image manualThreshold(Image image, int value) {
        return ImageManipulationHelper.threshold(prepareImage(image), value);
    }

    @Override
    public Image globalThreshold(Image image, int deltaT) {
        final Image prepared = prepareImage(image);
        int actualT = 255 / 2; // This should always be the middle value as the image is normalized
        boolean shouldContinue = true;
        while (shouldContinue) {
            final int newT = calculateNewThreshold(prepared, actualT);
            shouldContinue = Math.abs(actualT - newT) >= deltaT;
            actualT = newT;
        }
        return ImageManipulationHelper.threshold(prepared, actualT);
    }

    @Override
    public Image otsuThreshold(Image image) {
        final Image prepared = prepareImage(image);
        final Histogram histogram = ImageManipulationHelper.getHistogram(prepared, 0);
        final int min = histogram.minCategory();
        final int max = histogram.maxCategory();
        final Map<Integer, Double> cumulativeMeans = IntStream.range(min, max + 1)
                .boxed()
                .collect(Collectors.toMap(
                        Function.identity(),
                        t -> IntStream.range(min, t + 1).mapToDouble(i -> i * histogram.getFrequency(i)).sum())
                );
        final double globalMean = cumulativeMeans.get(max);
        final Map<Integer, Double> cumulativeFrequencies = IntStream.range(min, max + 1)
                .boxed()
                .collect(Collectors.toMap(
                        Function.identity(),
                        t -> IntStream.range(min, t + 1).mapToDouble(histogram::getFrequency).sum())
                );
        final Map<Integer, Double> variances =
                IntStream.range(min, max + 1)
                        .boxed()
                        .map(t -> {
                            final double class1Probability = cumulativeFrequencies.get(t);
                            // In case the probability is 0 or 1,
                            // then all pixels are in one class, so the variance is zero.
                            if (class1Probability == 0 || class1Probability == 1) {
                                return new AbstractMap.SimpleEntry<>(t, 0d); // TODO: make sure this is OK
                            }
                            final double class2Probability = 1 - class1Probability;
                            final double class1Mean = cumulativeMeans.get(t);
                            final double numerator = Math.pow(globalMean * class1Probability - class1Mean, 2);
                            final double denominator = class1Probability * class2Probability;
                            return new AbstractMap.SimpleEntry<>(t, numerator / denominator);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final double maxVariance = variances.values().stream()
                .max(Comparator.comparingDouble(d -> d))
                .orElseThrow(() -> new RuntimeException("This should not happen"));
        final List<Integer> possibleThresholds = variances.entrySet().stream()
                .filter(e -> e.getValue() == maxVariance)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        // Use the median as the selected threshold
        return ImageManipulationHelper.threshold(prepared, possibleThresholds.get(possibleThresholds.size() / 2));
    }

    /**
     * Prepares the {@link Image} in order to make it eligible for the threshold function application.
     * This method transforms the {@link Image} into a gray {@link Image}, and then it normalizes it.
     *
     * @param image The {@link Image} to be prepared.
     * @return The prepared {@link Image}.
     */
    private static Image prepareImage(Image image) {
        return ImageManipulationHelper.normalize(ImageManipulationHelper.toGray(image));
    }

    /**
     * Calculates a new threshold value, according to the global threshold technique.
     *
     * @param image   The image to which the threshold will be calculated.
     * @param actualT The actual threshold (i.e used to calculate the new value).
     * @return The new threshold value.
     */
    private static int calculateNewThreshold(Image image, int actualT) {
        return IntStream.range(0, image.getWidth())
                .mapToObj(x -> IntStream.range(0, image.getHeight()).
                        boxed()
                        .collect(Collectors.groupingBy(y -> image.getSample(x, y, 0) <= (double) actualT ? 0 : 255,
                                new GroupSumCollector(x, image))))
                .collect(new NewThresholdCollector());
    }

    /**
     * A group average collector, to be used when grouping by image coordinates.
     *
     * @apiNote This {@link Collector} expects the {@link Image} to be prepared with the
     * {@link #prepareImage(Image)} method.
     */
    private static final class GroupSumCollector implements Collector<Integer, List<Integer>, List<Double>> {

        /**
         * The row to be collected.
         */
        private final int x;

        /**
         * The image from where pixel values are taken.
         */
        private final Image image;

        /**
         * Constructor.
         *
         * @param x     The row to be collected.
         * @param image The image from where pixel values are taken.
         */
        private GroupSumCollector(int x, Image image) {
            this.x = x;
            this.image = image;
        }

        @Override
        public Supplier<List<Integer>> supplier() {
            return LinkedList::new;
        }

        @Override
        public BiConsumer<List<Integer>, Integer> accumulator() {
            return List::add;
        }

        @Override
        public BinaryOperator<List<Integer>> combiner() {
            return (l1, l2) -> {
                l1.addAll(l2);
                return l1;
            };
        }

        @Override
        public Function<List<Integer>, List<Double>> finisher() {
            return list -> list.stream()
                    .map(y -> image.getSample(x, y, 0))
                    .collect(Collectors.toList());
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.singleton(Characteristics.CONCURRENT);
        }
    }

    /**
     * A {@link Collector} that takes pixel values by row
     * (separated between those that would be black and those that would be white),
     * and returns a new threshold value (according to the global threshold technique).
     */
    private static final class NewThresholdCollector
            implements Collector<Map<Integer, List<Double>>, Map<Integer, List<Double>>, Integer> {

        @Override
        public Supplier<Map<Integer, List<Double>>> supplier() {
            return HashMap::new;
        }

        @Override
        public BiConsumer<Map<Integer, List<Double>>, Map<Integer, List<Double>>> accumulator() {
            return (accumulator, processed) -> {
                // First validate that the processed map is compatible
                final int size = processed.size();
                if ((size == 2 && processed.containsKey(0) && processed.containsKey(255))
                        || (size == 1 && (processed.containsKey(0) || processed.containsKey(255)))
                        || size == 0) {
                    mergeMaps(accumulator, processed);
                    return;
                }
                throw new IllegalArgumentException("Illegal processed map");
            };
        }

        @Override
        public BinaryOperator<Map<Integer, List<Double>>> combiner() {
            return NewThresholdCollector::mergeMaps;
        }

        @Override
        public Function<Map<Integer, List<Double>>, Integer> finisher() {
            return map -> {
                final double m1 = map.get(0).stream().mapToDouble(i -> i).average().orElse(0);
                final double m2 = map.get(255).stream().mapToDouble(i -> i).average().orElse(0);
                return (int) ((m1 + m2) / 2);
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.singleton(Characteristics.CONCURRENT);
        }

        /**
         * Merges the two {@link Map}s.
         *
         * @param collectorMap The {@link Map} collecting values.
         * @param partialMap   The {@link Map} whose values will be merged into the {@code collectorMap}.
         * @return The merged {@link Map}.
         */
        private static Map<Integer, List<Double>> mergeMaps(Map<Integer, List<Double>> collectorMap,
                                                            Map<Integer, List<Double>> partialMap) {
            // Group 1: the black pixels
            final List<Double> partialGroup1 = partialMap.getOrDefault(0, new LinkedList<>());
            collectorMap.merge(0, partialGroup1, NewThresholdCollector::mergeFunction);
            // Group 2: the white pixels
            final List<Double> partialGroup2 = partialMap.getOrDefault(255, new LinkedList<>());
            collectorMap.merge(255, partialGroup2, NewThresholdCollector::mergeFunction);
            // The result map
            return collectorMap;
        }

        /**
         * Merges the two {@link List}s, adding all the elements of the second one into the first one.
         *
         * @param actual The actual list (the one accumulating).
         * @param newOne The new list (the one with more elements to add).
         * @return The list with all the elements.
         */
        private static List<Double> mergeFunction(List<Double> actual, List<Double> newOne) {
            actual.addAll(newOne);
            return actual;
        }
    }
}
