package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.JobConditionValue;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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
        setFieldIfExist(node, "error", JsonNode::asText, obj::setError);
        setFieldIfExist(node, "lastUpdateTimestamp", JsonNode::asText, obj::setLastUpdateTimestamp);
        setFieldIfExist(node, "value", JsonNode::asBoolean, obj::setValue);
        setFieldIfExist(node, "slotIds", this::slotIds, obj::setSlotIds);
        setFieldIfExist(node, "numberOfSlotsRequired", JsonNode::asInt, obj::setNumberOfSlotsRequired);
        setFieldIfExist(node, "slotsName", this::namespacedName, obj::setSlotsName);
        setFieldIfExist(node, "jobName", this::namespacedName, obj::setJobName);
        setFieldIfExist(node, "jobs", this::jobs, obj::setJobs);

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

    private Set<JobConditionValue> jobs(JsonNode node) {
        if (!node.isArray())
            throw new RuntimeException("Not an array");
        var set = new HashSet<JobConditionValue>();
        for (JsonNode jobConditionNode : node) {
            var key = jobConditionNode.get("name");
            var value = jobConditionNode.get("value");

            set.add(new JobConditionValue(namespacedName(key), value.asBoolean()));
        }

        return set;
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
        if (condition == null || !SchedulingCondition.constructorMap.containsKey(condition))
            return null;

        return SchedulingCondition.constructorMap.get(condition).get();
    }
}
