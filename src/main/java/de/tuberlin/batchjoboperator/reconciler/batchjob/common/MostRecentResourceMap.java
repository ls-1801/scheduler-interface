package de.tuberlin.batchjoboperator.reconciler.batchjob.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.apache.commons.lang3.math.NumberUtils;

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
     * ({resourceVersion:2}).updateAndReturnOld({resourceVersion:1}) returns resourceVersion:2 and keeps {
     * resourceVersion:2}
     * ({resourceVersion:2}).updateAndReturnOld({resourceVersion:3}) returns resourceVersion:2 and progress to {
     * resourceVersion:3}
     * ({resourceVersion:3}).updateAndReturnOld({resourceVersion:2}) returns resourceVersion:3 and keeps {
     * resourceVersion:3}
     *
     * @return Will return the old resource with the highest ResourceVersion, however this happens regardless of the
     * given resource. If no recent Version exists
     */
    public T updateAndReturnOld(T resource) {
        var ref = managerMap.computeIfAbsent(resource.getMetadata().getUid(), (k) -> new AtomicReference<>());

        final var resourceVersion = NumberUtils.toLong(resource.getMetadata().getResourceVersion(), 0);
        Optional<T> mostRecent;
        boolean doSwap;
        do {
            mostRecent = Optional.ofNullable(ref.get());
            var mostRecentVersion = mostRecent.map(HasMetadata::getMetadata)
                                              .map(ObjectMeta::getResourceVersion)
                                              .map(rv -> NumberUtils.toLong(rv, 0))
                                              .orElse(0L);
            doSwap = resourceVersion > mostRecentVersion;
        } while (doSwap && !ref.compareAndSet(mostRecent.orElse(null), resource));

        return mostRecent.orElse(resource);
    }

    public void remove(T mostRecent) {
        managerMap.remove(mostRecent.getMetadata().getUid());
    }
}
