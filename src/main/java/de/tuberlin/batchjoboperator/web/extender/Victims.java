package de.tuberlin.batchjoboperator.web.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.batchjoboperator.reconciler.slots.ApplicationPodView;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
@RequiredArgsConstructor
public class Victims {
    @JsonProperty("Pods")
    List<Pod> pods;
    @JsonProperty("NumPDBViolations")
    Long numPDBViolations;

    @Override
    public String toString() {
        return "Victims{" +
                "pods=" + pods.stream().map(ApplicationPodView::wrap).collect(Collectors.toList()) +
                ", numPDBViolations=" + numPDBViolations +
                '}';
    }
}
