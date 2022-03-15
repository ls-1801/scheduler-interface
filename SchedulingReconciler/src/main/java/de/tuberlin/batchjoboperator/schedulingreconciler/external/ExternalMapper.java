package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotScheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotSchedulingItem;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface ExternalMapper {


    @Mapping(source = "metadata.name", target = "name")
    @Mapping(source = "status.state", target = "state")
    @Mapping(source = "status.scheduledEvents", target = "scheduledEvents")
    @Mapping(source = "spec.externalScheduler", target = "externalScheduler")
    ExternalBatchJob toExternal(BatchJob internal);

    @InheritInverseConfiguration
    BatchJob toInternal(ExternalBatchJob external);

    @Mapping(source = "spec.slotBased", target = "slots")
    @Mapping(source = "spec.queueBased", target = "queue")
    @Mapping(source = "status.jobStates", target = "jobStatus")
    @Mapping(source = "metadata.name", target = "name")
    ExternalScheduling toExternal(Scheduling internal);

    @InheritInverseConfiguration
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "spec.slots", ignore = true)
    Scheduling toInternal(ExternalScheduling external, @Context NamespacedName slots, @Context String namespace);

    @AfterMapping
    default void calledWithSourceAndTarget(ExternalScheduling external, @MappingTarget Scheduling internal,
                                           @Context NamespacedName slots, @Context String namespace) {
        internal.getSpec().setSlots(slots);
    }

    @Mapping(source = "jobs", target = "jobs")
    @Mapping(source = "mode", target = "mode")
    ExternalSlotScheduling toExternal(SlotScheduling internal);

    @InheritInverseConfiguration
    SlotScheduling toInternal(ExternalSlotScheduling internal, @Context String namespace);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "slotIds", target = "slots")
    ExternalSlotSchedulingItems toExternal(SlotSchedulingItem internal);

    @InheritInverseConfiguration
    SlotSchedulingItem toInternal(ExternalSlotSchedulingItems external, @Context String namespace);

    default String toExternal(NamespacedName name) {
        return name.getName();
    }

    default NamespacedName toInternal(String name, @Context String namespace) {
        return new NamespacedName(name, namespace);
    }

    default ExternalBatchJobSchedulingStatusWithoutDuplicates map(Set<SchedulingJobState> value) {
        var set = value.stream()
                       .map(js -> new ExternalBatchJobSchedulingStatus(js.getName().getName(), js.getState()))
                       .collect(Collectors.toSet());

        return new ExternalBatchJobSchedulingStatusWithoutDuplicates(set);
    }

    default Set<SchedulingJobState> map(ExternalBatchJobSchedulingStatusWithoutDuplicates value) {
        return Collections.emptySet();
    }
}
