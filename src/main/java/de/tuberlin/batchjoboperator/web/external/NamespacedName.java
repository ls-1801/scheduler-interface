package de.tuberlin.batchjoboperator.web.external;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Data;

@Data
public class NamespacedName {
    private String namespace;
    private String name;

    public NamespacedName(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public NamespacedName() {
    }

    public static NamespacedName of(HasMetadata meta) {
        return new NamespacedName(meta.getMetadata().getNamespace(), meta.getMetadata().getName());
    }
}
