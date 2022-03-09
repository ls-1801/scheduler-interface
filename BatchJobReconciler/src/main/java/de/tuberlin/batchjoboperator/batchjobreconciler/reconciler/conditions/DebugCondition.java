package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.primitives.Longs;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.common.util.MostRecentResourceMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.zjsonpatch.JsonDiff;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static java.util.Optional.ofNullable;

@Slf4j
public class DebugCondition extends BatchJobCondition {
    public static final String condition = DEBUG_CONDITION;
    @JsonIgnore
    private static final MostRecentResourceMap<HasMetadata> mostRecentApplication = new MostRecentResourceMap<>();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public String getCondition() {
        return condition;
    }

    private boolean resourceVersionChanged(HasMetadata old, HasMetadata current) {
        var oldRV = ofNullable(old)
                .map(HasMetadata::getMetadata)
                .map(ObjectMeta::getResourceVersion)
                .map(Longs::tryParse).orElse(0L);

        var currentRV = ofNullable(current)
                .map(HasMetadata::getMetadata)
                .map(ObjectMeta::getResourceVersion)
                .map(Longs::tryParse).orElse(0L);

        boolean didChange = currentRV > oldRV;

        if (didChange) {
            log.info("Application has changed");
        }

        return didChange;
    }

    @SneakyThrows
    @Override
    protected boolean updateInternal(BatchJobContext context) {
        var current = context.getApplication().getApplication();
        var old = mostRecentApplication.updateIfNewerAndReturnOld(current);
        if (resourceVersionChanged(old, current)) {
            return false;
        }

        var prev = mapper.valueToTree(old);
        var recent = mapper.valueToTree(current);
        log.info("DIFF: {}", mapper.writeValueAsString(JsonDiff.asJson(prev, recent)));
        return false;
    }
}
