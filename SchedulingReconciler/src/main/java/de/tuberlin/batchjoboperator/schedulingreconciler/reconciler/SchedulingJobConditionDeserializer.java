package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_COMPLETION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_ENQUEUE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_START;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_SUBMISSION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_NUMBER_OF_SLOTS_AVAILABLE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_PRECEDING_JOB_SUBMISSION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_SLOTS_AVAILABLE;

public class SchedulingJobConditionDeserializer extends StdDeserializer<AbstractSchedulingJobCondition> {

    public SchedulingJobConditionDeserializer() {
        this(null);
    }

    public SchedulingJobConditionDeserializer(Class<?> vc) {
        super(vc);
    }

    public static SimpleModule getModule() {
        var module = new SimpleModule();
        module.addDeserializer(AbstractSchedulingJobCondition.class, new SchedulingJobConditionDeserializer());
        return module;
    }

    @Override
    public AbstractSchedulingJobCondition deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        var obj = constructClassBasedOnCondition(node.get("condition").asText());

        if (obj == null) {
            ctxt.reportInputMismatch(AbstractSchedulingJobCondition.class, "missing condition");
            return null;
        }

        setFieldIfExist(node, "condition", JsonNode::asText, obj::setCondition);
        setFieldIfExist(node, "name", this::namespacedName, obj::setName);
        setFieldIfExist(node, "error", JsonNode::asText, obj::setError);
        setFieldIfExist(node, "lastUpdateTimestamp", JsonNode::asText, obj::setLastUpdateTimestamp);
        setFieldIfExist(node, "value", JsonNode::asBoolean, obj::setValue);
        setFieldIfExist(node, "slotsIds", this::slotIds, obj::setSlotIds);
        setFieldIfExist(node, "numberOfSlotsRequired", JsonNode::asInt, obj::setNumberOfSlotsRequired);
        setFieldIfExist(node, "slotsName", this::namespacedName, obj::setSlotsName);


        return obj;
    }

    private <T> void setFieldIfExist(JsonNode node, String propertyName, Function<JsonNode, T> fn,
                                     Consumer<T> consumer) {
        if (node.has(propertyName) && !node.get(propertyName).isNull()) {
            consumer.accept(fn.apply(node.get(propertyName)));
        }
    }

    private NamespacedName namespacedName(JsonNode node) {
        var name = node.get("name").asText();
        var namespace = node.get("namespace").asText();

        return new NamespacedName(name, namespace);
    }

    private Set<Integer> slotIds(JsonNode node) {
        var set = new HashSet<Integer>();
        for (JsonNode jsonNode : node) {
            set.add(jsonNode.asInt());
        }
        return set;
    }

    @Nullable
    private AbstractSchedulingJobCondition constructClassBasedOnCondition(@Nullable String condition) {
        if (condition == null)
            return null;

        switch (condition) {
            case AWAITING_JOB_SUBMISSION:
                return new AwaitingJobSubmissionCondition();


            case AWAITING_NUMBER_OF_SLOTS_AVAILABLE:
                return new AwaitNumberOfSlotsAvailableCondition();


            case AWAITING_SLOTS_AVAILABLE:
                return new AwaitSlotsAvailableCondition();


            case AWAITING_JOB_COMPLETION:
                return new AwaitingJobCompletionCondition();


            case AWAITING_JOB_ENQUEUE:
                return new AwaitingJobEnqueueCondition();


            case AWAITING_PRECEDING_JOB_SUBMISSION:
                return new AwaitPreviousJobSubmittedCondition();

            case AWAITING_JOB_START:
                return new AwaitingJobStartCondition();

        }

        return null;
    }
}
