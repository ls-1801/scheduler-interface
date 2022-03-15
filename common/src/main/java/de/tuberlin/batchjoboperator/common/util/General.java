package de.tuberlin.batchjoboperator.common.util;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
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

    public static <T> T logAndRethrow(Supplier<T> runnable) {
        try {
            return runnable.get();
        } catch (Exception e) {
            log.error("Exception thrown", e);
            e.printStackTrace();
            throw e;
        }
    }

    public static void logAndRethrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Exception thrown", e);
            e.printStackTrace();
            throw e;
        }
    }

    public static boolean nonNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }

    @Nonnull
    public static <T> T requireFirstElement(@Nonnull Set<T> ts) {
        return Objects.requireNonNull(CollectionUtils.firstElement(ts));
    }

    @Nonnull
    public static <T> T requireFirstElement(@Nonnull List<T> ts) {
        return Objects.requireNonNull(CollectionUtils.firstElement(ts));
    }
}
