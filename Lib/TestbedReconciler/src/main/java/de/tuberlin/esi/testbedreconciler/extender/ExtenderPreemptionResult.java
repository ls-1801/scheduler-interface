package de.tuberlin.esi.testbedreconciler.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Map;

@Value
@RequiredArgsConstructor
public class ExtenderPreemptionResult {
    @JsonProperty("NodeNameToMetaVictims")
    Map<String, MetaVictims> nodeNameToMetaVictims;
}
