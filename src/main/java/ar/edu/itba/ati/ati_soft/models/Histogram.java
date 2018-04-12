package ar.edu.itba.ati.ati_soft.models;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an {@link Image} histogram for a given band.
 */
public class Histogram {

    /**
     * The total amount (i.e used to calculate the frequency.
     */
    private final long amount;
    /**
     * A {@link Map} holding, for each category, the corresponding value.
     */
    private final Map<Integer, Long> values;

    /**
     * Constructor.
     *
     * @param series A {@link Map} holding, for each category, the corresponding value.
     */
    public Histogram(Map<Integer, Long> series) {
        this.amount = series.values().stream().mapToLong(l -> l).sum();
        this.values = new HashMap<>(series);
    }

    /**
     * Gets the count for a given {@code category}.
     *
     * @param category The category whose count will be returned.
     * @return The category count.
     */
    public long getCount(int category) {
        return Optional.ofNullable(values.get(category)).orElse(0L);
    }

    /**
     * Gets the frequency for a given category.
     *
     * @param category The category whose frequency will be returned.
     * @return The category frequency.
     */
    public double getFrequency(int category) {
        return Optional.ofNullable(values.get(category)).map(v -> (double) v / amount).orElse(0D);
    }

    /**
     * @return The first category.
     */
    public int minCategory() {
        return values.keySet().stream().min(Comparator.comparingInt(o -> o))
                .orElseThrow(() -> new IllegalStateException("Empty Histogram"));
    }

    /**
     * @return The last category.
     */
    public int maxCategory() {
        return values.keySet().stream().max(Comparator.comparingInt(o -> o))
                .orElseThrow(() -> new IllegalStateException("Empty Histogram"));
    }
}
