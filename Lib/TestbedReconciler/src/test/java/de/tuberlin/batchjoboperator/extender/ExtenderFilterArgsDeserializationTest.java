package de.tuberlin.batchjoboperator.extender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.NodeListBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExtenderFilterArgsDeserializationTest {
    @Test
    void name() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var uut = ExtenderFilterArgs.builder()
                                    .nodeNames(Set.of("node1"))
                                    .pod(new PodBuilder()
                                            .withNewMetadata()
                                            .withName("Pod1")
                                            .withUid(UUID.randomUUID().toString())
                                            .endMetadata().withNewSpec()
                                            .addNewContainer()
                                            .withName("nginx")
                                            .withImage("nginx")
                                            .endContainer()
                                            .endSpec()
                                            .build())
                                    .nodes(new NodeListBuilder()
                                            .addNewItem()
                                            .withNewMetadata()
                                            .withName("node1")
                                            .endMetadata()
                                            .endItem()
                                            .build())
                                    .build();

        assertThat(mapper.readValue(mapper.writeValueAsString(uut), ExtenderFilterArgs.class))
                .isEqualTo(uut);

    }
}