package de.tuberlin.esi.schedulingreconciler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.esi.common.crd.scheduling.JobConditionValue;
import de.tuberlin.esi.schedulingreconciler.statemachine.AwaitJobsAcquiredCondition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingJobConditionDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TestDeserialization {
    @Test
    void test() throws JsonProcessingException {
        var condition = new AwaitJobsAcquiredCondition();

        condition.setJobs(Set.of(new JobConditionValue(new NamespacedName("job1", "default"), false)));

        var mapper = new ObjectMapper().registerModule(SchedulingJobConditionDeserializer.getModule());

        var string = mapper.writeValueAsString(condition);
        log.info("{}", string);


        var conditionDes = mapper.readValue(string, AbstractSchedulingJobCondition.class);

        assertThat(conditionDes)
                .isInstanceOf(AwaitJobsAcquiredCondition.class)
                .extracting("jobs")
                .isEqualTo(Sets.set(new JobConditionValue(new NamespacedName("job1", "default"), false)));

    }
}
