package ar.edu.itba.ati.ati_soft.utils;

import java.util.Objects;

/**
 * Defines behaviour for an object that consumer four elements.
 *
 * @param <T> The type of the first element.
 * @param <U> The type of the second element.
 * @param <R> The type of the third element.
 * @param <S> The type of the fourth element.
 */
@FunctionalInterface
public interface QuadConsumer<T, U, R, S> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param t The first input argument
     * @param u The second input argument
     * @param r The third input argument
     * @param s The fourth input argument
     */
    void accept(T t, U u, R r, S s);

    /**
     * Returns a composed {@code {@link QuadConsumer }} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after The operation to perform after this operation
     * @return a composed {@code {@link QuadConsumer }} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException If {@code after} is null
     */
    default QuadConsumer<T, U, R, S> andThen(QuadConsumer<? super T, ? super U, ? super R, ? super S> after) {
        Objects.requireNonNull(after);
        return (t, u, r, s) -> {
            accept(t, u, r, s);
            after.accept(t, u, r, s);
        };
    }
}
