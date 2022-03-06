package de.tuberlin.batchjoboperator;

import de.tuberlin.batchjoboperator.slotsreconciler.SlotReconciler;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {

    @Bean
    public SlotReconciler slotReconciler(KubernetesClient client) {
        return new SlotReconciler(client);
    }

    @Bean
    public KubernetesClient client() {
        return new DefaultKubernetesClient();
    }

    // Register all controller beans
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Operator operator(List<Reconciler> controllers) {
        Operator operator = new Operator(DefaultConfigurationService.instance());
        controllers.forEach(operator::register);
        return operator;
    }
}