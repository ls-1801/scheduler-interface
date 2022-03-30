package de.tuberlin.esi.schedulingreconciler.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.esi.common.reconcilers.CustomClonerConfigurationService;
import de.tuberlin.esi.schedulingreconciler.SchedulingReconciler;
import de.tuberlin.esi.schedulingreconciler.external.ExternalMapper;
import de.tuberlin.esi.schedulingreconciler.external.ExternalMapperImpl;
import de.tuberlin.esi.schedulingreconciler.external.ExternalSchedulerInterfaceController;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingJobConditionDeserializer;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@Configuration
public class Config {

    @Value("${NAMESPACE:default}")
    private String namespace;

    @Bean
    ExternalSchedulerInterfaceController interfaceController(KubernetesClient client,
                                                             SimpMessagingTemplate template,
                                                             ExternalMapper mapper) {
        return new ExternalSchedulerInterfaceController(client, template, mapper, namespace);
    }


    @Bean
    public SchedulingReconciler schedulingReconciler(KubernetesClient client) {
        return new SchedulingReconciler(client, namespace);
    }

    @Bean
    public ExternalMapper externalMapper() {
        return new ExternalMapperImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(SchedulingJobConditionDeserializer.getModule());
    }

    @Bean
    public KubernetesClient client() {
        var client = new DefaultKubernetesClient();
        Serialization.jsonMapper().registerModule(SchedulingJobConditionDeserializer.getModule());
        return client;
    }

    // Register all controller beans
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Operator operator(List<Reconciler> controllers, ObjectMapper objectMapper) {
        var configService = new CustomClonerConfigurationService(objectMapper);
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