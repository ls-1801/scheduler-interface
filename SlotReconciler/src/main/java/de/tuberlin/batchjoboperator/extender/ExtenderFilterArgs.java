package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.batchjoboperator.slotsreconciler.ApplicationNodeView;
import de.tuberlin.batchjoboperator.slotsreconciler.ApplicationPodView;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.Set;
import java.util.stream.Collectors;

@Value
@RequiredArgsConstructor
@ToString
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

