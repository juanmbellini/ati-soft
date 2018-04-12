package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.HistogramService;
import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.service.StatsHelper.StatsContainer;
import ar.edu.itba.ati.ati_soft.utils.AccumulatorCollector;
import ar.edu.itba.ati.ati_soft.utils.QuadFunction;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Concrete implementation of {@link HistogramService}.
 */
@Service
public class HistogramServiceImpl implements HistogramService {

    @Override
    public Map<Integer, Histogram> getHistograms(Image image) {
        return IntStream.range(0, image.getBands())
                .parallel()
                .boxed()
                .collect(Collectors.toMap(b -> b, b -> getHistogram(image, b)));
    }

    @Override
    public Histogram getCumulativeDistributionHistogram(Histogram histogram) {
        final Map<Integer, Double> frequencies = IntStream.range(histogram.minCategory(), histogram.maxCategory() + 1)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), histogram::getFrequency));
        final Map<Integer, Double> cdf = cdf(frequencies);
        final Double min = cdf.values().stream()
                .min(Comparator.comparingDouble(o -> o))
                .orElseThrow(() -> new RuntimeException("This should not happen."));
        final double maxPixel = histogram.maxCategory();

        final Map<Integer, Long> better = cdf(frequencies).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> (long) (((entry.getValue() - min) / (1.0 - min)) * maxPixel + 0.5)));
        return new Histogram(better);
    }

    @Override
    public Image increaseContrast(Image image) {
        final StatsContainer[] stats = StatsHelper.getStats(image, HistogramServiceImpl::getHistogram);
        final double minimums[] = Arrays.stream(stats)
                .mapToDouble(StatsContainer::getMin)
                .toArray();
        final double maximums[] = Arrays.stream(stats)
                .mapToDouble(StatsContainer::getMax)
                .toArray();
        final double r1s[] = Arrays.stream(stats)
                .mapToDouble(stat ->
                        rProvider(stat, (m, v) -> m - v, r -> r >= stat.getMin(), (m, v) -> stat.getMin() + m / 2))
                .toArray();
        final double r2s[] = Arrays.stream(stats)
                .mapToDouble(stat ->
                        rProvider(stat, (m, v) -> m + v, r -> r <= stat.getMax(), (m, v) -> stat.getMax() - m / 2))
                .toArray();
        final double s1s[] = IntStream.range(0, stats.length)
                .mapToDouble(b -> minimums[b] + r1s[b])
                .map(val -> val / 2)
                .toArray();
        final double s2s[] = IntStream.range(0, stats.length)
                .mapToDouble(b -> maximums[b] + r2s[b])
                .map(val -> val / 2)
                .toArray();

        final QuadFunction<Double, Double, Double, Double, Function<Double, Double>> toLinear = toLinear();
        final BiFunction<Integer, Double, Double> f1 = (b, v) -> toLinear.andThen(linear -> linear.apply(v))
                .apply(minimums[b], minimums[b], r1s[b], s1s[b]);
        final BiFunction<Integer, Double, Double> f2 = (b, v) -> toLinear.andThen(linear -> linear.apply(v))
                .apply(r1s[b], s1s[b], r2s[b], s2s[b]);
        final BiFunction<Integer, Double, Double> f3 = (b, v) -> toLinear.andThen(linear -> linear.apply(v))
                .apply(r2s[b], s2s[b], maximums[b], maximums[b]);

        final BiFunction<Integer, Double, Double> newValueFunction = parted(f1, minimums, f2, maximums, f3);
        return ImageManipulationHelper.createApplying(image, (x, y, b, v) -> newValueFunction.apply(b, v));
    }


    @Override
    public Image equalize(Image image) {
        final Map<Integer, Histogram> cumulativeHistograms = getHistograms(image).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> getCumulativeDistributionHistogram(e.getValue())));
        return ImageManipulationHelper.createApplying(image,
                (x, y, b, v) -> (double) cumulativeHistograms.get(b).getCount((v.intValue())));
    }

    /**
     * Calculates the {@link Histogram} of the given {@link Image}, for the given {@code band}.
     *
     * @param image The {@link Image} whose {@link Histogram} will be calculated.
     * @param band  The band to be calculated.
     * @return The calculated {@link Histogram}.
     */
    private static Histogram getHistogram(Image image, int band) {
        final Map<Integer, Long> values = IntStream.range(0, image.getWidth())
                .parallel()
                .mapToObj(x -> IntStream.range(0, image.getHeight())
                        .parallel()
                        .mapToObj(y -> image.getSample(x, y, band)))
                .flatMap(Function.identity())
                .map(Double::intValue)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return new Histogram(values);
    }

    /**
     * Calculates the cumulative distribution for the given frequencies.
     *
     * @param frequencies A {@link Map} holding, for each value, the frequency it has.l
     * @return A {@link Map} containing the cumulative distribution.
     */
    private static Map<Integer, Double> cdf(Map<Integer, Double> frequencies) {
        final List<Integer> categories = frequencies.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        final List<Double> accumulated = frequencies.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(new AccumulatorCollector<>(0d, (o1, o2) -> o1 + o2));
        return IntStream.range(0, frequencies.size())
                .boxed()
                .collect(Collectors.toMap(categories::get, accumulated::get));
    }

    /**
     * Creates {@link QuadFunction} that takes two points (in x1, y2, x2, y2 format), and builds a linear function.
     *
     * @return A {@link QuadFunction} that takes two points (in x1, y2, x2, y2 format), and builds a linear function.
     */
    private static QuadFunction<Double, Double, Double, Double, Function<Double, Double>> toLinear() {
        return (x1, y1, x2, y2) -> {
            final double m = (y2 - y1) / (x2 - x1);
            final double b = y1 - m * x1;
            return x -> m * x + b;
        };
    }

    /**
     * Builds a parted {@link BiFunction} from the given {@link BiFunction}s.
     * The integer argument in all {@link BiFunction} represents an array index,
     * that will be used in the given {@code lowerLimits} and {@code upperLimits}.
     *
     * @param f1          The function that takes place before the lower limit.
     * @param lowerLimits The lower limits.
     * @param f2          The function that takes place between lower and upper limits.
     * @param upperLimits The upper limits.
     * @param f3          The function that takes place after the upper limit.
     * @return
     */
    private static BiFunction<Integer, Double, Double> parted(BiFunction<Integer, Double, Double> f1,
                                                              double lowerLimits[],
                                                              BiFunction<Integer, Double, Double> f2,
                                                              double upperLimits[],
                                                              BiFunction<Integer, Double, Double> f3) {
        return (b, v) -> {
            if (v <= lowerLimits[b]) {
                return f1.apply(b, v);
            }
            if (v >= upperLimits[b]) {
                return f3.apply(b, v);
            }
            return f2.apply(b, v);
        };
    }

    /**
     * Provides an r value, according to the given {@code stats} and functions.
     *
     * @param stats    The {@link StatsContainer} from where data is taken.l
     * @param provider A {@link BiFunction} that takes the mean and variance, and provide a possible value.
     * @param tester   A {@link Predicate} that takes the possible value, and tells if it s a valid one or not.
     * @param adapter  A {@link BiFunction} that takes the mean an variance, and adapts the possible value.
     * @return The provided r value.
     */
    private static double rProvider(StatsContainer stats,
                                    BiFunction<Double, Double, Double> provider,
                                    Predicate<Double> tester,
                                    BiFunction<Double, Double, Double> adapter) {
        final double mean = stats.getMean();
        final double variance = stats.getVariance();
        final double possibleValue = provider.apply(mean, variance);
        return tester.test(possibleValue) ? possibleValue : adapter.apply(mean, variance);
    }
}
