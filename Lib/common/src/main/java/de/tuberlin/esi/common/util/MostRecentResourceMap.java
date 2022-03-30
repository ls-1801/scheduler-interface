package de.tuberlin.esi.common.util;

import com.google.common.primitives.Longs;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ThreadSafe
public class MostRecentResourceMap<T extends HasMetadata> {
    private final Map<String, AtomicReference<T>> managerMap = new ConcurrentHashMap<>();

    /**
     * This guarantees, that only the resource with the highest ResourceVersion is stored.
     *
     * @return Will return the old resource with the highest ResourceVersion or null if no recent version exists
     */
    @Nullable
    public T updateIfNewerAndReturnOld(@Nonnull T resource) {
        var ref = managerMap.computeIfAbsent(resource.getMetadata().getUid(), (k) -> new AtomicReference<>());

        final var resourceVersion = NumberUtils.toLong(resource.getMetadata().getResourceVersion(), 0);
        Optional<T> mostRecent;
        boolean doSwap;
        do {
            mostRecent = Optional.ofNullable(ref.get());
            var mostRecentVersion = mostRecent.map(HasMetadata::getMetadata)
                                              .map(ObjectMeta::getResourceVersion)
                                              .map(Longs::tryParse)
                                              .orElse(0L);
            doSwap = resourceVersion > mostRecentVersion;
        } while (doSwap && !ref.compareAndSet(mostRecent.orElse(null), resource));

        return mostRecent.orElse(null);
    }

    public void remove(T mostRecent) {
        managerMap.remove(mostRecent.getMetadata().getUid());
    }
}
