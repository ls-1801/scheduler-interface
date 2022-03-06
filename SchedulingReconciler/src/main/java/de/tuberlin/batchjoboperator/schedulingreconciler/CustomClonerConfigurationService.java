package de.tuberlin.batchjoboperator.schedulingreconciler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.config.Cloner;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import lombok.experimental.Delegate;

public class CustomClonerConfigurationService implements ConfigurationService {
    private final Cloner cloner;
    @Delegate(excludes = ExcludeCloner.class)
    private final DefaultConfigurationService delegate = DefaultConfigurationService.instance();

    public CustomClonerConfigurationService(ObjectMapper mapper) {
        this.cloner = new Cloner() {
            @Override
            public HasMetadata clone(HasMetadata object) {
                try {
                    return mapper.readValue(mapper.writeValueAsString(object), object.getClass());
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Override
    public Cloner getResourceCloner() {
        return this.cloner;
    }

    interface ExcludeCloner {
        Cloner getResourceCloner();
    }
}