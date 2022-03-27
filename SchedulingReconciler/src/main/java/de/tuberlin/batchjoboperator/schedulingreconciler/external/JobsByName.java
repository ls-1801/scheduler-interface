package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class JobsByName {
    @JsonValue
    private Map<String, ExternalBatchJob> value;

    @JsonCreator
    public JobsByName(Map<String, ExternalBatchJob> value) {
        this.value = value;
    }

    public JobsByName(List<ExternalBatchJob> jobs) {
        this.value = jobs.stream()
                         .collect(Collectors.toMap(ExternalBatchJob::getName, Function.identity()));
    }
}
