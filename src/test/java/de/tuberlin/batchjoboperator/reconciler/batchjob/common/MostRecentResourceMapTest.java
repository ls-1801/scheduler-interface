package de.tuberlin.batchjoboperator.reconciler.batchjob.common;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MostRecentResourceMapTest {

    private Pod withRV(String uuid, String resourceVersion) {
        return new PodBuilder()
                .withNewMetadata()
                .withResourceVersion(resourceVersion)
                .withUid(uuid).endMetadata().build();
    }

    @Test
    void testFunctionality() {
        var uut = new MostRecentResourceMap<Pod>();
        var podUID = UUID.randomUUID().toString();
        var pod8 = withRV(podUID, "8");
        var pod10 = withRV(podUID, "10");
        var pod12 = withRV(podUID, "12");
        var pod14 = withRV(podUID, "14");
        var pod16 = withRV(podUID, "16");

        assertThat(uut.updateIfNewerAndReturnOld(pod10))
                .isNull();

        assertThat(uut.updateIfNewerAndReturnOld(pod12))
                .isEqualTo(pod10);

        assertThat(uut.updateIfNewerAndReturnOld(pod12))
                .isEqualTo(pod12);

        assertThat(uut.updateIfNewerAndReturnOld(pod8))
                .isEqualTo(pod12);

        assertThat(uut.updateIfNewerAndReturnOld(pod8))
                .isEqualTo(pod12);

        assertThat(uut.updateIfNewerAndReturnOld(pod10))
                .isEqualTo(pod12);

        assertThat(uut.updateIfNewerAndReturnOld(pod14))
                .isEqualTo(pod12);

        assertThat(uut.updateIfNewerAndReturnOld(pod16))
                .isEqualTo(pod14);

    }
}