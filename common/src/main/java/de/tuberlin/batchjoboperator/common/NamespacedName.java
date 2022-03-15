package de.tuberlin.batchjoboperator.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Data;

import java.util.Objects;

@Data
public class NamespacedName {
    private String namespace;
    private String name;

    public NamespacedName(String name, String namespace) {
        this.namespace = Objects.requireNonNull(namespace);
        this.name = Objects.requireNonNull(name);
    }

    public NamespacedName() {
    }

    public static NamespacedName of(HasMetadata meta) {
        return new NamespacedName(meta.getMetadata().getName(), meta.getMetadata().getNamespace());
    }

    public static String getNamespace(HasMetadata meta) {
        return of(meta).getNamespace();
    }

    public static String getName(HasMetadata meta) {
        return of(meta).getName();
    }
}
