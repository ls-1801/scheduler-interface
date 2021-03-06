package de.tuberlin.esi.testbedreconciler.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.esi.testbedreconciler.DebugController;
import de.tuberlin.esi.testbedreconciler.extender.ExtenderController;
import de.tuberlin.esi.testbedreconciler.reconciler.TestbedReconciler;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.ControllerConfigurationOverrider;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {

    @Value("${NAMESPACE:default}")
    private String namespace;

    @Bean
    public DebugController debugController(KubernetesClient client, ObjectMapper mapper) {
        return new DebugController(client, mapper, namespace);
    }

    @Bean
    public ExtenderController extenderController(KubernetesClient client) {
        return new ExtenderController(client);
    }

    @Bean
    public TestbedReconciler TestbedReconciler(KubernetesClient client) {
        return new TestbedReconciler(client, namespace);
    }

    @Bean
    public KubernetesClient client() {
        return new DefaultKubernetesClient();
    }

    // Register all controller beans
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Operator operator(List<Reconciler> controllers) {
        var configurationService = DefaultConfigurationService.instance();
        configurationService.getClientConfiguration().setNamespace(namespace);
        Operator operator = new Operator(configurationService);


        controllers.forEach(controller -> {
            var controllerConfiguration =
                    ControllerConfigurationOverrider.override(configurationService.getConfigurationFor(controller))
                                                    .settingNamespace(namespace)
                                                    .build();

            operator.register(controller, controllerConfiguration);
        });
        return operator;
    }
}