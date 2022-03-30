package de.tuberlin.esi.testbedreconciler.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationPodView;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.stream.Collectors;

@Value
@RequiredArgsConstructor
@Builder
@Jacksonized
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
