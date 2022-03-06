package de.tuberlin.batchjoboperator.common.util;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class General {

    @Nonnull
    public static <T> Stream<Pair<T, Integer>> enumerate(@Nullable Collection<T> ts) {
        if (ts == null)
            return Stream.empty();

        return Streams.zip(ts.stream(), IntStream.range(0, ts.size()).boxed(), Pair::of);
    }

    public static <T> Optional<T> getNullSafe(Supplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (NullPointerException npex) {
            return Optional.empty();
        }
    }

    public static boolean nonNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }
}
