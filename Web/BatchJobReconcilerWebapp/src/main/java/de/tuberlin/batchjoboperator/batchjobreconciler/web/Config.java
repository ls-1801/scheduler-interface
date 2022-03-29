package de.tuberlin.batchjoboperator.batchjobreconciler.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobConditionDeserializer;
import de.tuberlin.batchjoboperator.common.reconcilers.CustomClonerConfigurationService;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {

    @Value("${NAMESPACE:default}")
    private String namespace;

    @Bean
    public BatchJobReconciler batchJobReconciler(KubernetesClient client) {
        return new BatchJobReconciler(client, namespace);
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
        configService.getClientConfiguration().setNamespace(namespace);
        Operator operator = new Operator(configService);
        controllers.forEach(controller -> {
            var controllerConfiguration = configService.getConfigurationFor(controller);
            controllerConfiguration.setConfigurationService(configService);
            var overrideNamespace = ControllerConfigurationOverrider.override(controllerConfiguration)
                                                                    .settingNamespace(namespace)
                                                                    .build();

            operator.register(controller, overrideNamespace);
        });
        return operator;
    }
}