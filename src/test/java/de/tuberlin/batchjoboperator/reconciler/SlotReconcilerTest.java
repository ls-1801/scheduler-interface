package de.tuberlin.batchjoboperator.reconciler;

import de.tuberlin.batchjoboperator.web.external.SchedulingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SlotReconcilerTest {
    @Mock
    SchedulingDecision schedulingDecision;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void name() {
        when(schedulingDecision.getDecision()).thenReturn(emptyMap());

        assertThat(schedulingDecision.getDecision()).isNotNull();
    }
}