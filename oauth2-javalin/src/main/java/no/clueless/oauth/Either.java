package no.clueless.oauth;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple implementation of the Either pattern.
 *
 * @param <L> the left type
 * @param <R> the right type
 */
public sealed interface Either<L, R> {

    /**
     * Maps the value of this Either instance to a new value.
     *
     * @param leftMapper  the mapper to apply to the left value
     * @param rightMapper the mapper to apply to the right value
     * @param <T>         the type of the mapped value
     * @return the mapped value
     */
    <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper);

    /**
     * Consumes the value of this Either instance.
     *
     * @param leftConsumer  the left consumer
     * @param rightConsumer the right consumer
     */
    void consume(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer);

    /**
     * Static factory method for creating a Left instance.
     *
     * @param value the value to wrap in the Either instance.
     * @param <L>   the left type
     * @param <R>   the right type
     * @return a Left instance.
     */
    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    /**
     * Static factory method for creating a Right instance.
     *
     * @param value the value to wrap in the Either instance.
     * @param <L>   the left type
     * @param <R>   the right type
     * @return a Right instance.
     */
    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    /**
     * Represents a Left value in the Either pattern.
     *
     * @param value the value to wrap in the Either instance.
     * @param <L>   the left type
     * @param <R>   the right type
     */
    record Left<L, R>(L value) implements Either<L, R> {
        @Override
        public <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper) {
            return leftMapper.apply(value);
        }

        @Override
        public void consume(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
            leftConsumer.accept(value);
        }
    }

    /**
     * Represents a Right value in the Either pattern.
     *
     * @param value the value to wrap in the Either instance.
     * @param <L>   the left type
     * @param <R>   the right type
     */
    record Right<L, R>(R value) implements Either<L, R> {
        @Override
        public <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper) {
            return rightMapper.apply(value);
        }

        @Override
        public void consume(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
            rightConsumer.accept(value);
        }
    }
}
