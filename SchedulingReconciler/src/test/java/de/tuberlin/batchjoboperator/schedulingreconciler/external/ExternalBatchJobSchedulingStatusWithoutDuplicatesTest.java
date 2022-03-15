package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.Completed;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.InQueue;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.Scheduled;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.Submitted;
import static org.assertj.core.api.Assertions.assertThat;


class ExternalBatchJobSchedulingStatusWithoutDuplicatesTest {
    private ExternalBatchJobSchedulingStatus event(String name, SchedulingJobStateEnum stateEnum) {
        return ExternalBatchJobSchedulingStatus.builder()
                                               .name(name)
                                               .state(stateEnum)
                                               .build();
    }

    @Test
    void testEnumOrder() {
        var withDuplicates = Set.of(
                event("name1", Scheduled),
                event("name1", Submitted),
                event("name1", InQueue),
                event("name1", Completed),

                event("name2", InQueue),
                event("name2", Completed),
                event("name2", Submitted),
                event("name2", Scheduled),

                event("name3", InQueue)
        );

        var without = new ExternalBatchJobSchedulingStatusWithoutDuplicates(withDuplicates);

        assertThat(without.getStatus())
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", Completed),
                        Tuple.tuple("name2", Completed),
                        Tuple.tuple("name3", InQueue)
                );

    }

    @Test
    void jsonMapping() throws JsonProcessingException {
        var withDuplicates = Set.of(
                event("name1", Scheduled),
                event("name1", Submitted),
                event("name1", InQueue),
                event("name1", Completed),

                event("name2", InQueue),
                event("name2", Completed),
                event("name2", Submitted),
                event("name2", Scheduled),

                event("name3", InQueue)
        );

        var without = new ExternalBatchJobSchedulingStatusWithoutDuplicates(withDuplicates);

        var mapper = new ObjectMapper();
        var jsonString = mapper.writeValueAsString(without);


        var set = mapper.readValue(jsonString, new TypeReference<Set<ExternalBatchJobSchedulingStatus>>() {
        });

        assertThat(set)
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", Completed),
                        Tuple.tuple("name2", Completed),
                        Tuple.tuple("name3", InQueue)
                );

        var uut = mapper.readValue(jsonString, ExternalBatchJobSchedulingStatusWithoutDuplicates.class);

        assertThat(uut.getStatus())
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", Completed),
                        Tuple.tuple("name2", Completed),
                        Tuple.tuple("name3", InQueue)
                );
    }
}