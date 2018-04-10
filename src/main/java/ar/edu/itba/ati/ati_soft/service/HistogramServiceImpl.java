package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.HistogramService;
import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;
import ar.edu.itba.ati.ati_soft.utils.AccumulatorCollector;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Concrete implementation of {@link HistogramService}.
 */
@Component
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
}
