package de.tuberlin.esi.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NodeName {
    @JsonValue
    private final String nodeName;
}
