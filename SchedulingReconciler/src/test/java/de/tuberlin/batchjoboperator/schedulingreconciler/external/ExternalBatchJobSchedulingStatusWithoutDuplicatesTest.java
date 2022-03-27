package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.COMPLETED;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.IN_QUEUE;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.SCHEDULED;
import static de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState.SchedulingJobStateEnum.SUBMITTED;
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
                event("name1", SCHEDULED),
                event("name1", SUBMITTED),
                event("name1", IN_QUEUE),
                event("name1", COMPLETED),

                event("name2", IN_QUEUE),
                event("name2", COMPLETED),
                event("name2", SUBMITTED),
                event("name2", SCHEDULED),

                event("name3", IN_QUEUE)
        );

        var without = new ExternalBatchJobSchedulingStatusWithoutDuplicates(withDuplicates);

        assertThat(without.getStatus())
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", COMPLETED),
                        Tuple.tuple("name2", COMPLETED),
                        Tuple.tuple("name3", IN_QUEUE)
                );

    }

    @Test
    void jsonMapping() throws JsonProcessingException {
        var withDuplicates = Set.of(
                event("name1", SCHEDULED),
                event("name1", SUBMITTED),
                event("name1", IN_QUEUE),
                event("name1", COMPLETED),

                event("name2", IN_QUEUE),
                event("name2", COMPLETED),
                event("name2", SUBMITTED),
                event("name2", SCHEDULED),

                event("name3", IN_QUEUE)
        );

        var without = new ExternalBatchJobSchedulingStatusWithoutDuplicates(withDuplicates);

        var mapper = new ObjectMapper();
        var jsonString = mapper.writeValueAsString(without);


        var set = mapper.readValue(jsonString, new TypeReference<Set<ExternalBatchJobSchedulingStatus>>() {
        });

        assertThat(set)
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", COMPLETED),
                        Tuple.tuple("name2", COMPLETED),
                        Tuple.tuple("name3", IN_QUEUE)
                );

        var uut = mapper.readValue(jsonString, ExternalBatchJobSchedulingStatusWithoutDuplicates.class);

        assertThat(uut.getStatus())
                .extracting("name", "state")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("name1", COMPLETED),
                        Tuple.tuple("name2", COMPLETED),
                        Tuple.tuple("name3", IN_QUEUE)
                );
    }
}