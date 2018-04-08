package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.HistogramService;
import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Component;

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
}
