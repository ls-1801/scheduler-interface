package de.tuberlin.esi.common;

import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class BatchJobStateTest {
    @Test
    void enumWorksCorrectly() {
        Assertions.assertThat(BatchJobState.valueOf("ReadyState")).isEqualTo(BatchJobState.ReadyState);
    }
}