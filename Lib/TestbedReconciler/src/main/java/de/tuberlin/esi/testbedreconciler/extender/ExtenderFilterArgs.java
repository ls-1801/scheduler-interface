package de.tuberlin.esi.testbedreconciler.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationNodeView;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationPodView;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;
import java.util.stream.Collectors;

@Value
@RequiredArgsConstructor
@Builder
@Jacksonized
public class ExtenderFilterArgs {
    @JsonProperty("Pod")
    Pod pod;
    @JsonProperty("Nodes")
    NodeList nodes;

    @JsonProperty("NodeNames")
    Set<String> nodeNames;

    public String toString() {
        return "ExtenderFilterArgs(pod=" + ApplicationPodView.wrap(pod) + ", " +
                "nodes=" + this.getNodes().getItems().stream().map(ApplicationNodeView::wrap)
                               .collect(Collectors.toList()) + ", " +
                "nodeNames" +
                "=" + this.nodeNames +
                ")";
    }

}

