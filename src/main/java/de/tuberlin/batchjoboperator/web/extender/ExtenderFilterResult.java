package de.tuberlin.batchjoboperator.web.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@RequiredArgsConstructor
public class ExtenderFilterResult {
    @JsonProperty("Pod")
    Pod pod;
    @JsonProperty("Nodes")
    NodeList nodes;

    @JsonProperty("NodeNames")
    List<String> nodeNames;

    @JsonProperty("FailedNodes")
    Map<String, String> failedNodesMap;

    @JsonProperty("FailedAndUnresolvableNodes")
    Map<String, String> failedAndUnresolvableNodesMap;

    @JsonProperty("Error")
    String error;

    public static ExtenderFilterResult error(String error) {
        return new ExtenderFilterResult(null, null, null, null, null, error);
    }
}
