package de.tuberlin.batchjoboperator.web.external;

import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ExternalSchedulerControllerTest {

    @Test
    void name() {
        assertNull(Ints.tryParse("GARBAGE"));
    }
}