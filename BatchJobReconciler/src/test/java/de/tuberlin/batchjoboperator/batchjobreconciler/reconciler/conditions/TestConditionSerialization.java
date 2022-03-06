package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.AbstractBatchJobCondition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_ENQUEUE_REQUEST_CONDITION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestConditionSerialization {

    @Test
    void castReference() {

    }

    @Test
    void schedulingCondtion() throws JsonProcessingException {
        var string = "[\n" +
                "  {\n" +
                "    \"condition\": \"AwaitingNumberOfSlotsAvailable\",\n" +
                "    \"lastUpdateTimestamp\": \"2022-03-03T16:10:35.027831\",\n" +
                "    \"numberOfSlotsRequired\": \"2\",\n" +
                "    \"name\": {\n" +
                "      \"name\": \"SparkJob1\",\n" +
                "      \"namespace\": \"SparkJob1\"\n" +
                "    },\n" + "    " +
                "    \"slotsName\": {\n" +
                "      \"name\": \"Slot1\",\n" +
                "      \"namespace\": \"Slot1\"\n" +
                "    },\n" +
                "    \"error\": \"yesError\",\n" +
                "    \"value\": true\n" +
                "  }\n" +
                "]";
        var desConditions = Serialization.jsonMapper().readValue(string,
                new TypeReference<Set<AbstractSchedulingJobCondition>>() {
                });

        assertThat(desConditions)
                .hasSize(1)
                .extracting("condition", "name.name", "name.namespace", "value", "error", "numberOfSlotsRequired",
                        "slotsName.name",
                        "slotsName.namespace")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("AwaitingNumberOfSlotsAvailable", "SparkJob1", "SparkJob1", true, "yesError", 2,
                                "Slot1", "Slot1"));
    }

    @Test
    void name() throws JsonProcessingException {
        var condition = new AwaitReleaseCondition();

        Serialization.jsonMapper().registerModule(BatchJobConditionDeserializer.getModule());

        var string = Serialization.jsonMapper().writeValueAsString(Set.of(condition));

        var desCondition = Serialization.jsonMapper().readValue(string,
                new TypeReference<Set<AbstractBatchJobCondition>>() {
                });

        assertThat(desCondition).first().isInstanceOf(AwaitReleaseCondition.class);

        var moreComplicatedString = "[\n" +
                "  {\n" +
                "    \"condition\": \"AWAIT_RUNNING_CONDITION\",\n" +
                "    \"name\": {\n" +
                "      \"name\": \"SparkJob1\",\n" +
                "      \"namespace\": \"SparkJob1\"\n" +
                "    },\n" +
                "    \"error\": \"yesError\",\n" +
                "    \"value\": false\n" +
                "  },\n" +
                "  {\n" +
                "    \"condition\": \"AWAIT_COMPLETION_CONDITION\",\n" +
                "    \"name\": {\n" +
                "      \"name\": \"SparkJob1\",\n" +
                "      \"namespace\": \"SparkJob1\"\n" +
                "    },\n" +
                "    \"value\": true\n" +
                "  }\n" +
                "]";

        var desConditions = Serialization.jsonMapper().readValue(moreComplicatedString,
                new TypeReference<Set<AbstractBatchJobCondition>>() {
                });

        assertThat(desConditions)
                .hasSize(2)
                .extracting("condition", "name.name", "name.namespace", "value", "error")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("AWAIT_RUNNING_CONDITION", "SparkJob1", "SparkJob1", false, "yesError"),
                        Tuple.tuple("AWAIT_COMPLETION_CONDITION", "SparkJob1", "SparkJob1", true, null));


        var castedSet = desConditions.stream()
                                     .map(a -> (BatchJobCondition) a)
                                     .collect(Collectors.toSet());

        castedSet.stream().forEach(c -> c.setValue(true));

        var updateString = Serialization.jsonMapper().writeValueAsString(desConditions);

        var desConditionsAfterCast = Serialization.jsonMapper().readValue(updateString,
                new TypeReference<Set<AbstractBatchJobCondition>>() {
                });

        assertThat(desConditionsAfterCast)
                .hasSize(2)
                .extracting("condition", "name.name", "name.namespace", "value", "error")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("AWAIT_RUNNING_CONDITION", "SparkJob1", "SparkJob1", true, "yesError"),
                        Tuple.tuple("AWAIT_COMPLETION_CONDITION", "SparkJob1", "SparkJob1", true, null));


    }

    @Test
    void testEqualsAndHashUsingSet() {
        var set = new HashSet<>();

        var job1 = new NamespacedName("job1", "default");
        var job2 = new NamespacedName("job2", "default");
        var job3 = new NamespacedName("job3", "default");
        var request1 = new AwaitEnqueueRequest();
        var request2 = new AwaitEnqueueRequest();
        var request3 = new AwaitEnqueueRequest();

        request1.setName(job1);
        request2.setName(job2);
        request3.setName(job3);

        set.add(request1);
        set.add(request2);
        set.add(request3);

        assertThat(set)
                .hasSize(3)
                .extracting("name.name", "condition", "value")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("job1", AWAIT_ENQUEUE_REQUEST_CONDITION, false),
                        Tuple.tuple("job2", AWAIT_ENQUEUE_REQUEST_CONDITION, false),
                        Tuple.tuple("job3", AWAIT_ENQUEUE_REQUEST_CONDITION, false)
                );

        // job1 has been enqueued
        var trueCondition = new AwaitEnqueueRequest();
        trueCondition.setName(job1);
        trueCondition.setValue(true);

        set.remove(trueCondition);
        set.add(trueCondition);

        assertThat(set)
                .hasSize(3)
                .extracting("name.name", "condition", "value")
                .containsExactlyInAnyOrder(
                        Tuple.tuple("job1", AWAIT_ENQUEUE_REQUEST_CONDITION, true),
                        Tuple.tuple("job2", AWAIT_ENQUEUE_REQUEST_CONDITION, false),
                        Tuple.tuple("job3", AWAIT_ENQUEUE_REQUEST_CONDITION, false)
                );


    }
}
