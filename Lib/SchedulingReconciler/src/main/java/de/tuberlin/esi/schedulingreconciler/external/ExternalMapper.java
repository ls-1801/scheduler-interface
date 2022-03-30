package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.scheduling.Scheduling;
import de.tuberlin.esi.common.crd.scheduling.SchedulingJobState;
import de.tuberlin.esi.common.crd.scheduling.SlotScheduling;
import de.tuberlin.esi.common.crd.scheduling.SlotSchedulingItem;
import de.tuberlin.esi.common.crd.testbed.SlotOccupationStatus;
import de.tuberlin.esi.common.crd.testbed.Testbed;
import org.mapstruct.Context;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper
public interface ExternalMapper {


    @Mapping(source = "metadata.name", target = "name")
    @Mapping(source = "status.slots", target = "slotsByNode")
    @Mapping(source = "status.state", target = "state")
    @Mapping(expression = "java(internal.getStatus().getSlots().size() / internal.getSpec().getSlotsPerNode())",
            target = "numberOfNodes")
    @Mapping(target = "numberOfSlotsPerNode", source = "spec.slotsPerNode")
    ExternalTestbed toExternal(Testbed internal);

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
    @Mapping(source = "spec.testbed", target = "testBed")
    @Mapping(source = "status.state", target = "state")
    ExternalScheduling toExternal(Scheduling internal);

    @InheritInverseConfiguration
    @Mapping(target = "status", ignore = true)
    Scheduling toInternal(ExternalScheduling external, @Context String namespace);

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


    default Map<String, Set<SlotOccupationStatus>> mapSlotStatus(Set<SlotOccupationStatus> slotOccupationStatuses) {
        return slotOccupationStatuses.stream().collect(Collectors.groupingBy(
                slotOccupationStatus -> slotOccupationStatus.getNodeName(),
                Collectors.toSet()
        ));
    }
}
