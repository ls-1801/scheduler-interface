package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nullable;
import java.util.List;

@Value
@Jacksonized
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExternalScheduling {
    String name;
    SchedulingState state;

    ExternalBatchJobSchedulingStatusWithoutDuplicates jobStatus;

    @Nullable
    List<String> queue;
    @Nullable
    ExternalSlotScheduling slots;

//    public static ExternalScheduling fromInternal(KubernetesClient client, Scheduling scheduling) {
//        var builder = ExternalScheduling.builder();
//        if (scheduling.getSpec().getQueueBased() != null) {
//            builder.queue(scheduling.getSpec().getQueueBased().stream().map(NamespacedName::getName)
//                                    .collect(Collectors.toList()));
//        }
//
//        if (scheduling.getSpec().getSlotBased() != null) {
//
//            var items = scheduling.getSpec().getSlotBased().getJobs().stream()
//                      .map(j -> ExternalSlotSchedulingItems.builder().slots(j.getSlotIds())
//                                                           .name(j.getName().getName()).build())
//                      .collect(Collectors.toList());
//
//            builder.slots(ExternalSlotScheduling.builder()
//                                                .mode(scheduling.getSpec().getSlotBased().getMode().getMode())
//                                                .jobs(items)
//                                                .build());
//        }
//
//        return builder.name(scheduling.getMetadata().getName())
//                      .jobStatus(scheduling.getStatus().getJobStates().stream()
//                                           .map(
//                                                   j -> client.resources(BatchJob.class)
//                                                              .inNamespace("default")
//                                                              .withName(j.getName().getName()).get()
//                                           ).map(ExternalBatchJob::fromInternal)
//                                           .collect(Collectors.toList()))
//                      .state(scheduling.getStatus().getState())
//                      .build();
//    }

}
