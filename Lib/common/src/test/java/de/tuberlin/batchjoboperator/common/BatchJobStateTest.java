package de.tuberlin.batchjoboperator.common;

import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchJobStateTest {
    @Test
    void enumWorksCorrectly() {
        Assertions.assertThat(BatchJobState.valueOf("ReadyState")).isEqualTo(BatchJobState.ReadyState);
    }
}