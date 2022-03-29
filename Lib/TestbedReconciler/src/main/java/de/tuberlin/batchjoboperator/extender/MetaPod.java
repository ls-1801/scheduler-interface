package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@RequiredArgsConstructor
@Builder
@Jacksonized
public class MetaPod {
    @JsonProperty("UID")
    String uid;
}
