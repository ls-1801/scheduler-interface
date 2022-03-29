package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExtenderPreemptionArgsDeserializationTest {
    @Test
    void shouldDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var args = new ExtenderPreemptionArgs(new PodBuilder()
                .withNewMetadata()
                .withName("Pod1")
                .withUid(UUID.randomUUID().toString())
                .endMetadata().withNewSpec()
                .addNewContainer()
                .withName("nginx")
                .withImage("nginx")
                .endContainer()
                .endSpec()
                .build(),
                Map.of("nodename", new Victims(
                        List.of(new PodBuilder()
                                        .withNewMetadata()
                                        .withName("Pod2")
                                        .withUid(UUID.randomUUID().toString())
                                        .endMetadata().withNewSpec()
                                        .addNewContainer()
                                        .withName("nginx")
                                        .withImage("nginx")
                                        .endContainer()
                                        .endSpec()
                                        .build(),
                                new PodBuilder()
                                        .withNewMetadata()
                                        .withName("Pod3")
                                        .withUid(UUID.randomUUID().toString())
                                        .endMetadata().withNewSpec()
                                        .addNewContainer()
                                        .withName("nginx")
                                        .withImage("nginx")
                                        .endContainer()
                                        .endSpec()
                                        .build()), 0L)),
                Map.of("nodename", new MetaVictims(List.of(new MetaPod(UUID.randomUUID().toString())), 0L))
        );

        assertThat(mapper.readValue(mapper.writeValueAsString(args), ExtenderPreemptionArgs.class))
                .isEqualTo(args);
    }
}