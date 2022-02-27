package de.tuberlin.batchjoboperator.reconciler.slots;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Node;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApplicationNodeView extends Node {
    @Delegate
    private final Node node;

    private ApplicationNodeView(Node pod) {
        this.node = pod;
    }

    @Nonnull
    public static ApplicationNodeView wrap(@Nonnull Node node) {
        if (node instanceof ApplicationNodeView)
            return (ApplicationNodeView) node;

        return new ApplicationNodeView(node);
    }

    public Optional<String> getLabel(String labelName) {
        return Optional.ofNullable(node.getMetadata().getLabels())
                       .map(map -> map.get(labelName));
    }

    public Map<String, String> getLabels() {
        if (node.getMetadata().getLabels() == null)
            node.getMetadata().setLabels(new HashMap<>());

        return node.getMetadata().getLabels();
    }

    @JsonIgnore
    public String getName() {
        return node.getMetadata().getName();
    }

    @Override
    public String toString() {
        return getName();
    }
}
