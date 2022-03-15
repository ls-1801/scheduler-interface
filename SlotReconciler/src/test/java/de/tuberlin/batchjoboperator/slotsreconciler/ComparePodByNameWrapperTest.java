package de.tuberlin.batchjoboperator.slotsreconciler;

import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComparePodByNameWrapperTest {

    private final MutableInt counter = new MutableInt();

    private ApplicationPodView createPod(String cpu, String memory) {
        var pod1 = new PodBuilder()
                .withNewMetadata()
                .withName("POD" + counter.incrementAndGet())
                .withUid(UUID.randomUUID().toString())
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withNewResources()
                .addToRequests("cpu", new Quantity(cpu))
                .addToRequests("memory", new Quantity(memory))
                .endResources()
                .endContainer()
                .endSpec()
                .build();

        return ApplicationPodView.wrap(pod1);
    }

    @Test
    void testFunctionality() {
        assertThat(new ComparePodByNameWrapper(createPod("200m", "4000M"))
                .equals(new ComparePodByNameWrapper(createPod("200m", "4000M"))))
                .isTrue();

        assertThat(new ComparePodByNameWrapper(createPod("200m", "4000M"))
                .equals(new ComparePodByNameWrapper(createPod("202m", "4002M"))))
                .isTrue();

        assertThat(new ComparePodByNameWrapper(createPod("200m", "4000M"))
                .equals(new ComparePodByNameWrapper(createPod("100m", "4000M"))))
                .isFalse();

        assertThat(new ComparePodByNameWrapper(createPod("200m", "4000M"))
                .equals(new ComparePodByNameWrapper(createPod("100m", "2000M"))))
                .isFalse();

        assertThat(new ComparePodByNameWrapper(createPod("200m", "4000M"))
                .equals(new ComparePodByNameWrapper(createPod("200m", "2000M"))))
                .isFalse();

        assertThat(new ComparePodByNameWrapper(createPod("125m", "896M"))
                .equals(new ComparePodByNameWrapper(createPod("125m", "512M"))))
                .isFalse();

    }
}