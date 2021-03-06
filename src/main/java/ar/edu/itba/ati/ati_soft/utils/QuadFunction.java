package ar.edu.itba.ati.ati_soft.utils;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a function that accepts four arguments and produces a result.
 * This is the four-arity specialization of {@link Function}.
 * <p>
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object, Object, Object, Object)}.
 *
 * @param <R> the type of the first argument to the function
 * @param <S> the type of the second argument to the function
 * @param <T> the type of the third argument to the function
 * @param <U> the type of the fourth argument to the function
 * @param <V> the type of the result of the function
 */
@FunctionalInterface
public interface QuadFunction<R, S, T, U, V> {

    /**
     * Applies this function to the given arguments.
     *
     * @param r the first function argument
     * @param s the second function argument
     * @param t the third function argument
     * @param u the fourth function argument
     * @return the function result
     */
    V apply(R r, S s, T t, U u);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <W>   the type of output of the {@code after} function, and of the
     *              composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <W> QuadFunction<R, S, T, U, W> andThen(Function<? super V, ? extends W> after) {
        Objects.requireNonNull(after);
        return (r, s, t, u) -> after.apply(apply(r, s, t, u));
    }
}
