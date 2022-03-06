package de.tuberlin.batchjoboperator.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Data;

@Data
public class NamespacedName {
    private String namespace;
    private String name;

    public NamespacedName(String name, String namespace) {
        this.namespace = namespace;
        this.name = name;
    }

    public NamespacedName() {
    }

    public static NamespacedName of(HasMetadata meta) {
        return new NamespacedName(meta.getMetadata().getName(), meta.getMetadata().getNamespace());
    }
}
