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

    <T> T map(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper);

    void consume(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer);

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

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
