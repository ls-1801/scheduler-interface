package de.tuberlin.batchjoboperator.batchjobreconciler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobConditionDeserializer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {


    @Bean
    public BatchJobReconciler batchJobReconciler(KubernetesClient client) {
        return new BatchJobReconciler(client);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(BatchJobConditionDeserializer.getModule());
    }

    @Bean
    public KubernetesClient client() {
        var client = new DefaultKubernetesClient();
        Serialization.jsonMapper().registerModule(BatchJobConditionDeserializer.getModule());
        return client;
    }

    // Register all controller beans
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Operator operator(List<Reconciler> controllers, ObjectMapper objectMapper) {
        var configService = new CustomClonerConfigurationService(objectMapper);
        Operator operator = new Operator(configService);
        controllers.forEach(c -> {
            var controllerConfiguration = configService.getConfigurationFor(c);
            controllerConfiguration.setConfigurationService(configService);
            operator.register(c, controllerConfiguration);
        });
        return operator;
    }
}