package de.tuberlin.batchjoboperator.web.extender;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
class MetaPod {
    @JsonProperty("UID")
    String uid;
}
