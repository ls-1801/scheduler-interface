package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Value
@RequiredArgsConstructor
@Builder
@Jacksonized
public class CreationRequest {
    Set<Integer> slotIds;
    NamespacedName slotsName;
    int replication;
}
