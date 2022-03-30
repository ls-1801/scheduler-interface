package de.tuberlin.esi.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Delegate;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class ExternalBatchJobSchedulingStatusWithoutDuplicates {
    @JsonValue
    Set<ExternalBatchJobSchedulingStatus> status;

    @JsonCreator
    public ExternalBatchJobSchedulingStatusWithoutDuplicates(Set<ExternalBatchJobSchedulingStatus> status) {
        this.status = status.stream()
                            .map(OverrideEquals::new)
                            .sorted(Comparator.comparing(OverrideEquals::getState).reversed())
                            .distinct()
                            .map(OverrideEquals::getDelegate)
                            .collect(Collectors.toSet());
    }

    @RequiredArgsConstructor
    private static class OverrideEquals {
        @Getter
        @Delegate
        private final ExternalBatchJobSchedulingStatus delegate;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OverrideEquals that = (OverrideEquals) o;
            return Objects.equals(delegate.getName(), that.delegate.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(delegate.getName());
        }
    }
}
