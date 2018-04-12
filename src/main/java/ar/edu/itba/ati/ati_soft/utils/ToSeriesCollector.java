package ar.edu.itba.ati.ati_soft.utils;

import javafx.scene.chart.XYChart;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A {@link Collector} that takes {@link XYChart.Data} objects and collects them into a {@link XYChart.Series}.
 *
 * @param <X> The 'X' axis objects type.
 * @param <Y> The 'Y' axis objects type.
 */
public final class ToSeriesCollector<X, Y>
        implements Collector<XYChart.Data<X, Y>, List<XYChart.Data<X, Y>>, XYChart.Series<X, Y>> {
    @Override
    public Supplier<List<XYChart.Data<X, Y>>> supplier() {
        return LinkedList::new;
    }

    @Override
    public BiConsumer<List<XYChart.Data<X, Y>>, XYChart.Data<X, Y>> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<XYChart.Data<X, Y>>> combiner() {
        return (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
    }

    @Override
    public Function<List<XYChart.Data<X, Y>>, XYChart.Series<X, Y>> finisher() {
        return l -> {
            final XYChart.Series<X, Y> series = new XYChart.Series<>();
            series.getData().addAll(l);
            return series;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.CONCURRENT);
    }
}
