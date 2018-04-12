package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.models.Histogram;
import ar.edu.itba.ati.ati_soft.models.Image;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * Class implementing several stats methods and algorithms.
 */
/* package */ class StatsHelper {


    /**
     * Returns an array containing the stats, for each band of the given {@link Image}.
     *
     * @param image               The {@link Image} whose stats will be calculated.
     * @param toHistogramFunction A {@link BiFunction} that takes an image and an index,
     *                            and returns an {@link Histogram}.
     *                            Will be called with the given {@link Image} and each band of the image.
     * @return An array with the stats of the image, for each band.
     * @apiNote The index of the array indicates which band the corresponding {@link StatsContainer} belongs to.
     */
    /* package */
    static StatsContainer[] getStats(Image image, BiFunction<Image, Integer, Histogram> toHistogramFunction) {
        final int bands = image.getBands();
        return IntStream.range(0, bands)
                .mapToObj(i -> toHistogramFunction.andThen(StatsContainer::new).apply(image, i))
                .toArray(StatsContainer[]::new);
    }

    /**
     * Bean class holding together the mean and variance obtained from an {@link Histogram}.
     */
    /* package */ static final class StatsContainer {

        /**
         * The first element.
         */
        private final int min;

        /**
         * The last element.
         */
        private final int max;

        /**
         * The contained mean.
         */
        private final double mean;

        /**
         * The contained variance.
         */
        private final double variance;

        /**
         * Constructor.
         *
         * @param histogram The {@link Histogram} from where data will be taken.
         */
        /* package */ StatsContainer(Histogram histogram) {
            this.min = histogram.minCategory();
            this.max = histogram.maxCategory();
            this.mean = IntStream.range(histogram.minCategory(), histogram.maxCategory() + 1)
                    .mapToDouble(level -> level * histogram.getFrequency(level))
                    .sum();
            this.variance = IntStream.range(histogram.minCategory(), histogram.maxCategory() + 1)
                    .mapToDouble(level -> (level - this.mean) * (level - this.mean) * histogram.getFrequency(level))
                    .sum();
        }

        /**
         * @return The contained mean.
         */
        /* package */ double getMean() {
            return mean;
        }

        /**
         * @return The contained variance.
         */
        /* package */ double getVariance() {
            return variance;
        }

        /**
         * @return The first element.
         */
        /* package */ int getMin() {
            return min;
        }

        /**
         * @return The last element.
         */
        /* package */ int getMax() {
            return max;
        }
    }
}
