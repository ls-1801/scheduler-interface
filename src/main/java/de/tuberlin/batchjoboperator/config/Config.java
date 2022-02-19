package de.tuberlin.batchjoboperator.config;

import de.tuberlin.batchjoboperator.reconciler.batchjob.BatchJobReconciler;
import de.tuberlin.batchjoboperator.reconciler.batchjob.spark.SparkApplicationManagerService;
import de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler;
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
    public BatchJobReconciler batchJobReconciler(KubernetesClient client,
                                                 SparkApplicationManagerService sparkApplicationManagerService) {
        return new BatchJobReconciler(client, sparkApplicationManagerService);
    }

    @Bean
    public SlotReconciler slotServiceController(KubernetesClient client) {
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