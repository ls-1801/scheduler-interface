package de.tuberlin.batchjoboperator.crd.batchjob;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import io.fabric8.kubernetes.api.model.Taint;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BatchJobStatus extends ObservedGenerationAwareStatus {
    private BatchJobState state;
    @Nullable
    private Long latestResourceVersion;
    private List<ScheduledEvents> scheduledEvents;
    @Nullable
    private Taint taint;
    @Nullable
    private SlotOccupationStatus slot;

    public BatchJobStatus() {
        this.state = BatchJobState.InQueueState;
        this.scheduledEvents = new ArrayList<>();
    }

    @JsonProperty
    public Long getObservedGeneration() {
        return super.getObservedGeneration();
    }
}
