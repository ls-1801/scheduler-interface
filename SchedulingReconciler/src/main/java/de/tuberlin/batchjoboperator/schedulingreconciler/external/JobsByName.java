package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobsByName {
    @JsonValue
    Map<String, ExternalBatchJob> value;

    public JobsByName(List<ExternalBatchJob> jobs) {
        this.value = jobs.stream()
                         .collect(Collectors.toMap(ExternalBatchJob::getName, Function.identity()));
    }
}
