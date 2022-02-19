package de.tuberlin.batchjoboperator.web.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@RequiredArgsConstructor
@ToString
public class ExtenderArgs {
    @JsonProperty("Pod")
    Pod pod;
    @JsonProperty("Nodes")
    NodeList nodes;

    @JsonProperty("NodeNames")
    List<String> nodeNames;

}

