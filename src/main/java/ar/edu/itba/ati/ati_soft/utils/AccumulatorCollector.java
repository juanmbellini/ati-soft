package ar.edu.itba.ati.ati_soft.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Collector} that collects elements by summing all the previous elements to them
 * (i.e given an element in the resuling {@link List}, that element was produced by a given value,
 * plus all the elements that are previous to it in the list).
 */
public class AccumulatorCollector<T> implements Collector<T, List<T>, List<T>> {

    /**
     * The identity of the sum (i.e a value that when added to a given element, the result is the given element).
     */
    private final T identity;

    /**
     * A {@link BiFunction} that takes to elements of type {@code T}, and returns the sum of them.
     */
    private final BiFunction<T, T, T> sumFunction;

    /**
     * Constructor.
     *
     * @param identity    The identity of the sum
     *                    (i.e a value that when added to a given element, the result is the given element).
     * @param sumFunction A {@link BiFunction} that takes to elements of type {@code T}, and returns the sum of them.
     */
    public AccumulatorCollector(T identity, BiFunction<T, T, T> sumFunction) {
        this.identity = identity;
        this.sumFunction = sumFunction;
    }

    @Override
    public Supplier<List<T>> supplier() {
        return LinkedList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return (l, v) -> {
            final T last = l.isEmpty() ? identity : l.get(l.size() - 1);
            l.add(sumFunction.apply(v, last));
        };
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (l1, l2) -> {
            final T total = l1.get(l1.size() - 1);
            return Stream.concat(l1.stream(), l2.stream()
                    .map(n -> sumFunction.apply(n, total)))
                    .collect(Collectors.toList());
        };
    }

    @Override
    public Function<List<T>, List<T>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        final Set<Characteristics> characteristics = new HashSet<>();
        characteristics.add(Characteristics.IDENTITY_FINISH);
        characteristics.add(Characteristics.CONCURRENT);
        return characteristics;
    }
}
