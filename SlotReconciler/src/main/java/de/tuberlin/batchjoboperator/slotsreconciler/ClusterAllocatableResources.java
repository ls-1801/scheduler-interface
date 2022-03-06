package de.tuberlin.batchjoboperator.slotsreconciler;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Quantity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class ClusterAllocatableResources {

    Map<String, Map<String, BigDecimal>> allocatableResourcesMap;

    private static Map<String, BigDecimal> quantityToBytes(Map<String, Quantity> quantities) {
        return quantities.entrySet().stream().collect(
                toMap(
                        Map.Entry::getKey,
                        resourcePerMapEntry -> Quantity.getAmountInBytes(resourcePerMapEntry.getValue()))
        );
    }


    public static ClusterAllocatableResources aggregate(List<Node> nodes) {
        var map = nodes
                .stream()
                .collect(
                        toMap(
                                node -> node.getMetadata().getName(),
                                node -> quantityToBytes(node.getStatus().getAllocatable())
                        )
                );

        var instance = new ClusterAllocatableResources();
        instance.allocatableResourcesMap = map;

        return instance;
    }
}
