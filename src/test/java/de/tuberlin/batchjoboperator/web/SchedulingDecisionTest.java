package de.tuberlin.batchjoboperator.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.web.external.SchedulingDecision;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingDecisionTest {

    @Test
    void deserialize() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        var uut = objectMapper.readerFor(SchedulingDecision.class)
                              .readValue("{\n" +
                                      "  \"gke-opcluster-default-pool-244fc649-2gqw\": [\n" +
                                      "    {\n" +
                                      "      \"namespace\": \"default\",\n" +
                                      "      \"name\": \"batchjob-sample\"\n" +
                                      "    }\n" +
                                      "  ]\n" +
                                      "}");

        assertThat(uut).isNotNull().extracting("decision").isNotNull();
    }
}