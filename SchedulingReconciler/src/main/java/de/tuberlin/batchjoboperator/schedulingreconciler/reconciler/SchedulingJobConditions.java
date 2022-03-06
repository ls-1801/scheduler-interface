package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SchedulingJobConditions {
    private static final ObjectMapper mapper = new ObjectMapper();
    private Set<SchedulingJobCondition> conditions = new HashSet<>();

    public static SchedulingJobConditions of(Scheduling scheduling) {
        var instance = new SchedulingJobConditions();
        instance.conditions = scheduling.getStatus().getConditions()
                                        .stream()
                                        .map(c -> (SchedulingJobCondition) c)
                                        .collect(Collectors.toSet());
        return instance;
    }

    public SchedulingJobConditions addCondition(SchedulingJobCondition condition) {
        // Using a Set here prevents duplicates
        // Since hash/equals are overloaded remove makes sure that the new condition is put into the set
        conditions.remove(condition);
        conditions.add(condition);
        return this;
    }

    public SchedulingJobConditions removeCondition(NamespacedName name, Set<String> conditions) {
        var remove = this.conditions.stream()
                                    .filter(c -> Objects.requireNonNull(c.getName()).equals(name))
                                    .filter(isCondition(conditions))
                                    .collect(Collectors.toSet());
        this.conditions.removeAll(remove);

        return this;
    }

    public void updateAll(KubernetesClient client) {
        conditions.forEach(c -> c.update(client));
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    public Boolean anyError() {
        return conditions.stream().anyMatch(c -> c.getError() != null);
    }


    private Predicate<SchedulingJobCondition> isCondition(Set<String> conditions) {
        return (condition) -> conditions.contains(mapper.valueToTree(condition).get("condition").asText());
    }

    public Set<NamespacedName> getRunnable() {
        var grouped = conditions.stream()
                                .filter(SchedulingJobCondition::preventRunnable)
                                .collect(Collectors.groupingBy(SchedulingJobCondition::getName, Collectors.toSet()));

        return grouped.entrySet().stream()
                      .filter(e -> e.getValue().stream().allMatch(SchedulingJobCondition::getValue))
                      .map(Map.Entry::getKey)
                      .collect(Collectors.toSet());
    }

    public Set<NamespacedName> anyWaiting(Set<String> conditions) {
        return this.conditions.stream()
                              .filter(isCondition(conditions))
                              .filter(condition -> !condition.getValue())
                              .map(SchedulingJobCondition::getName)
                              .collect(Collectors.toSet());
    }

    public Set<NamespacedName> withConditions(Set<String> conditions) {
        return this.conditions.stream()
                              .filter(isCondition(conditions))
                              .filter(SchedulingJobCondition::getValue)
                              .map(SchedulingJobCondition::getName)
                              .collect(Collectors.toSet());
    }

    public boolean anyAwaitSubmission() {
        return conditions.stream()
                         .filter(SchedulingJobCondition::preventSubmission)
                         .anyMatch(c -> !c.getValue());
    }

    public void setResourceConditions(Scheduling resource) {
        var conditions = this.conditions.stream()
                                        .map(c -> (AbstractSchedulingJobCondition) c)
                                        .collect(Collectors.toSet());
        resource.getStatus().setConditions(conditions);
    }
}
