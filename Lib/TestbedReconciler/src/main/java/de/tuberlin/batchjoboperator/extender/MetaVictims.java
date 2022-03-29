package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@RequiredArgsConstructor
@Builder
@Jacksonized
public class MetaVictims {
    @JsonProperty("Pods")
    List<MetaPod> pods;
    @JsonProperty("NumPDBViolations")
    Long numPDBViolations;

}
