package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import com.google.common.primitives.Ints;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_REPLICATION;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOT_IDS;
import static de.tuberlin.batchjoboperator.common.util.General.nonNull;

@Value
public class CreationRequest {
    Set<Integer> slotIds;
    NamespacedName slotsName;
    int replication;

    private CreationRequest(Set<Integer> slotIds, NamespacedName slotsName, int replication) {
        this.slotIds = slotIds;
        this.slotsName = slotsName;
        this.replication = replication;
    }

    @Nullable
    public static CreationRequest fromLabels(@Nullable Map<String, String> labelsMap) {
        if (labelsMap == null)
            return null;

        var slotsName = labelsMap.get(APPLICATION_CREATION_REQUEST_SLOTS_NAME);
        var slotsNamespace = labelsMap.get(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE);
        var slotIdsString = labelsMap.get(APPLICATION_CREATION_REQUEST_SLOT_IDS);
        var replication = labelsMap.get(APPLICATION_CREATION_REQUEST_REPLICATION);

        if (!nonNull(slotsName, slotsNamespace, slotIdsString, replication) && Ints.tryParse(replication) != null)
            return null;

        return new CreationRequest(
                SlotIDsAnnotationString.parse(slotIdsString).getSlotIds(),
                new NamespacedName(slotsName, slotsNamespace),
                Ints.tryParse(replication));
    }
}
