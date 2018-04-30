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
                .collect(Collectors.toMap(b -> b, b -> ImageManipulationHelper.getHistogram(image, b)));
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
        final StatsContainer[] stats = StatsHelper.getStats(image, ImageManipulationHelper::getHistogram);
        final double minimums[] = Arrays.stream(stats)
                .mapToDouble(StatsContainer::getMin)
                .toArray();
        final double maximums[] = Arrays.stream(stats)
                .mapToDouble(StatsContainer::getMax)
                .toArray();
        final double r1s[] = Arrays.stream(stats)
                .mapToDouble(stat -> rProvider(stat,
                        (m, v) -> m - Math.sqrt(v),
                        r -> r >= stat.getMin(),
                        (m, v) -> stat.getMin() + Math.sqrt(v) / 2))
                .toArray();
        final double r2s[] = Arrays.stream(stats)
                .mapToDouble(stat -> rProvider(stat,
                        (m, v) -> m + Math.sqrt(v),
                        r -> r <= stat.getMax(),
                        (m, v) -> stat.getMax() - Math.sqrt(v) / 2))
                .toArray();
        final double s1s[] = IntStream.range(0, stats.length)
                .mapToDouble(b -> {
                    final double min = minimums[b];
                    final double r1 = r1s[b];
                    // r1 is always bigger than min.
                    return (r1 - min) / 2 + min;
                })
                .toArray();
        final double s2s[] = IntStream.range(0, stats.length)
                .mapToDouble(b -> {
                    final double max = maximums[b];
                    final double r2 = r2s[b];
                    // max is always bigger than r2.
                    return (max - r2) / 2 + r2;
                })
                .toArray();

        final List<Function<Double, Double>> f1s = IntStream.range(0, stats.length)
                .mapToObj(b -> toLinear(minimums[b], minimums[b], r1s[b], s1s[b]))
                .collect(Collectors.toList());
        final List<Function<Double, Double>> f2s = IntStream.range(0, stats.length)
                .mapToObj(b -> toLinear(r1s[b], s1s[b], r2s[b], s2s[b]))
                .collect(Collectors.toList());
        final List<Function<Double, Double>> f3s = IntStream.range(0, stats.length)
                .mapToObj(b -> toLinear(r2s[b], s2s[b], maximums[b], maximums[b]))
                .collect(Collectors.toList());

        final List<Function<Double, Double>> partedFunctions = IntStream.range(0, stats.length)
                .mapToObj(b -> parted(f1s.get(b), minimums[b], f2s.get(b), maximums[b], f3s.get(b)))
                .collect(Collectors.toList());
        final BiFunction<Integer, Double, Double> newValueFunction = (b, v) -> partedFunctions.get(b).apply(v);
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
    private static Function<Double, Double> toLinear(double x1, double y1, double x2, double y2) {
        final double m = (y2 - y1) / (x2 - x1);
        final double b = y1 - m * x1;
        return x -> m * x + b;
    }

    /**
     * Builds a parted {@link Function} from the given {@link Function}s.
     *
     * @param f1         The function that takes place before the lower limit.
     * @param lowerLimit The lower limit.
     * @param f2         The function that takes place between lower and upper limits.
     * @param upperLimit The upper limit.
     * @param f3         The function that takes place after the upper limit.
     * @return The parted {@link Function}.
     */
    private static Function<Double, Double> parted(Function<Double, Double> f1, double lowerLimit,
                                                   Function<Double, Double> f2, double upperLimit,
                                                   Function<Double, Double> f3) {
        return v -> {
            if (v <= lowerLimit) {
                return f1.apply(v);
            }
            if (v >= upperLimit) {
                return f3.apply(v);
            }
            return f2.apply(v);
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
