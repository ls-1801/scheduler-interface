package de.tuberlin.batchjoboperator.reconciler.slots;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotsStatusState;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.util.QuantityUtil.getQuantityFromBytes;

@Value
public class SlotProblems {
    ImmutableMap<String, ImmutableList<Problem>> nodeProblems;
    ImmutableList<Problem> problems;


    private SlotProblems(ImmutableMap<String, ImmutableList<Problem>> nodeProblems, ImmutableList<Problem> problems) {
        this.nodeProblems = nodeProblems;
        this.problems = problems;
    }

    public static SlotProblemsBuilder builder() {
        return new SlotProblemsBuilder();
    }

    public boolean anyProblems() {
        return !problems.isEmpty() || !nodeProblems.isEmpty();
    }

    public UpdateControl<Slot> updateStatusIfRequired(Slot slot) {
        if (!anyProblems()) {
            slot.getStatus().setProblems(null);
            slot.getStatus().setNodeProblems(null);
            return UpdateControl.updateStatus(slot);
        }


        var nodeProblemsStringMap =
                nodeProblems.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    e -> e.getValue().stream().map(Problem::getMessage)
                                          .collect(Collectors.toList())));

        var problemsString =
                problems.stream()
                        .map(Problem::getMessage)
                        .collect(Collectors.toList());

        slot.getStatus().setNodeProblems(nodeProblemsStringMap);
        slot.getStatus().setProblems(problemsString);
        slot.getStatus().setState(SlotsStatusState.ERROR);

        return UpdateControl.updateStatus(slot);
    }

    @RequiredArgsConstructor
    @Getter
    static class Problem {
        private final String message;
    }

    static class NotASingleNodeControlledBySlotProblem extends Problem {
        public NotASingleNodeControlledBySlotProblem(String labelSelector) {
            super(MessageFormat.format("Not a single node found with label: {0}", labelSelector));
        }
    }

    @Getter
    static class NotEnoughRequestedResourcesProblem extends Problem {
        private final String resourceName;
        private final Quantity required;
        private final Quantity available;

        public NotEnoughRequestedResourcesProblem(String resourceName, Quantity required, Quantity available) {
            super(MessageFormat.format("Requires {1} of {0} resources. Available: {2}", resourceName, required,
                    available));
            this.resourceName = resourceName;
            this.required = required;
            this.available = available;
        }

        public NotEnoughRequestedResourcesProblem(String resourceName, BigDecimal required, BigDecimal available) {
            this(resourceName, getQuantityFromBytes(required), getQuantityFromBytes(available));
        }
    }

    public static class SlotProblemsBuilder {
        private final ImmutableMap.Builder<String, ImmutableList<Problem>> nodeProblems = new ImmutableMap.Builder<>();
        private final ImmutableList.Builder<Problem> problems = new ImmutableList.Builder<>();

        SlotProblemsBuilder() {
        }

        public SlotProblemsBuilder addNodeProblems(Node node, List<Problem> problem) {
            if (problem.isEmpty())
                return this;
            nodeProblems.put(node.getMetadata().getName(), ImmutableList.copyOf(problem));
            return this;
        }

        public SlotProblems build() {
            return new SlotProblems(nodeProblems.build(), problems.build());
        }

        public String toString() {
            return "SlotProblems.SlotProblemsBuilder()";
        }
    }
}
