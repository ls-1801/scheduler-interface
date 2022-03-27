package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@RequiredArgsConstructor
public class MetaVictims {
    @JsonProperty("Pods")
    List<MetaPod> pods;
    @JsonProperty("NumPDBViolations")
    Long numPDBViolations;

}
