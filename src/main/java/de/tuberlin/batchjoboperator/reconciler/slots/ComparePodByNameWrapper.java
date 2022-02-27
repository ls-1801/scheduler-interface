package de.tuberlin.batchjoboperator.reconciler.slots;

import io.fabric8.kubernetes.api.model.Quantity;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Getter
public class ComparePodByNameWrapper {
    transient private final ApplicationPodView pod;


    public ComparePodByNameWrapper(ApplicationPodView pod) {
        this.pod = pod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparePodByNameWrapper that = (ComparePodByNameWrapper) o;
        return Objects.equals(pod.getSlotId(), that.pod.getSlotId()) &&
                this.requestsMatchWithDelta(that.getPod().getRequestMap(), 5);
    }

    private boolean requestsMatchWithDelta(Map<String, Quantity> other, double delta) {
        return other.keySet().stream().allMatch(resourceName ->
        {
            var thisResource = this.pod.getRequestMap().getOrDefault(resourceName, new Quantity("0m"));
            var thatResource = other.getOrDefault(resourceName, new Quantity("0m"));
            var thisBytes = Quantity.getAmountInBytes(thisResource);
            var thatBytes = Quantity.getAmountInBytes(thatResource);


            var upper = thatBytes.compareTo(thisBytes.multiply(BigDecimal.valueOf(1 + (delta / 100) / 2)));
            var lower = thatBytes.compareTo(thisBytes.multiply(BigDecimal.valueOf(1 - (delta / 100) / 2)));
            return upper < 0 && lower > 0;
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(pod.getRequestMap(), pod.getSlotId());
    }
}
