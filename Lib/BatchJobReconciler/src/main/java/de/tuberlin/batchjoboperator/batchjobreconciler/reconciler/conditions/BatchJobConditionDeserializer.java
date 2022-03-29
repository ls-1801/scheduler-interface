package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.AbstractBatchJobCondition;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Deserializer is required for Jackson to know which SubClass of AbstractBatchJobCondition to construct
 * Construction is rather simple:
 * 1. Invoke the DefaultConstructor from the ConstructorMap
 * 2. Call any relevant setters if it is found inside the JSON
 * <p>
 * There is no validation happening and missing values inside the JSON will most likley cause NullPointers
 * in subsequent use of the Condition
 * <p>
 * TODO: Plenty of shared code between different Condition Deserializer
 */
public class BatchJobConditionDeserializer extends StdDeserializer<AbstractBatchJobCondition> {

    public BatchJobConditionDeserializer() {
        this(null);
    }

    public BatchJobConditionDeserializer(Class<?> vc) {
        super(vc);
    }

    public static SimpleModule getModule() {
        var module = new SimpleModule();
        module.addDeserializer(AbstractBatchJobCondition.class, new BatchJobConditionDeserializer());
        return module;
    }

    @Override
    public AbstractBatchJobCondition deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        var obj = constructClassBasedOnCondition(node.get("condition").asText());

        if (obj == null) {
            ctxt.reportInputMismatch(AbstractBatchJobCondition.class, "missing condition");
            return null;
        }

        setFieldIfExist(node, "condition", JsonNode::asText, obj::setCondition);
        setFieldIfExist(node, "name", this::namespacedName, obj::setName);
        setFieldIfExist(node, "error", JsonNode::asText, obj::setError);
        setFieldIfExist(node, "lastUpdateTimestamp", JsonNode::asText, obj::setLastUpdateTimestamp);
        setFieldIfExist(node, "value", JsonNode::asBoolean, obj::setValue);

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

    @Nullable
    private AbstractBatchJobCondition constructClassBasedOnCondition(@Nullable String condition) {
        if (condition == null)
            return null;

        if (!BatchJobCondition.constructors.containsKey(condition)) {
            throw new RuntimeException("Cannot construct BatchJob condition for: " + condition);
        }

        return BatchJobCondition.constructors.get(condition).get();
    }
}
