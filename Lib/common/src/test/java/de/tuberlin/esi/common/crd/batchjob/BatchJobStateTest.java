package de.tuberlin.esi.common.crd.batchjob;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchJobStateTest {
    @Test
    void enumWorksCorrectly() {
        assertThat(BatchJobState.valueOf("ReadyState")).isEqualTo(BatchJobState.ReadyState);
    }
}