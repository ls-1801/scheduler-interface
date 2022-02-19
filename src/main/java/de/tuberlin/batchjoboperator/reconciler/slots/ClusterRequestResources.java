package de.tuberlin.batchjoboperator.reconciler.slots;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class ClusterRequestResources {

    Map<String, Map<String, BigDecimal>> requestedResourceMap;

    private static Map<String, BigDecimal> quantityToBytes(Map<String, Quantity> quantities) {
        return quantities.entrySet().stream().collect(
                toMap(
                        Map.Entry::getKey,
                        resourcePerMapEntry -> Quantity.getAmountInBytes(resourcePerMapEntry.getValue()))
        );
    }

    private static Map<String, BigDecimal> aggregateRequestedResourcesPerNode(List<Pod> pods) {
        return pods.stream()
                   // Requested Resources on Container Level not Pods
                   .flatMap(podsPerNode -> podsPerNode.getSpec()
                                                      .getContainers()
                                                      .stream())
                   // Map container to Resource Requests
                   .map(containerPerNode -> containerPerNode.getResources()
                                                            .getRequests())
                   // Some might be null
                   .filter(Objects::nonNull)
                   .map(ClusterRequestResources::quantityToBytes)
                   // Reduce Stream of RequestedResources to a single map summing over each requested Resource
                   // Quantities
                   .reduce(new HashMap<>(), (map, resourcePerNode) -> {
                       resourcePerNode.forEach((resourceName, quantity) -> {
                           var current = map.getOrDefault(resourceName, BigDecimal.valueOf(0));
                           var updated = current.add(quantity);
                           map.put(resourceName, updated);
                       });
                       return map;
                   });
    }

    public static ClusterRequestResources aggregate(List<Pod> pods) {
        var map = pods
                .stream()
                // Ignore non-scheduled pods
                .filter(pod -> pod.getSpec().getNodeName() != null)
                //Group Pods by NodeName
                .collect(Collectors.groupingBy(pod -> pod.getSpec().getNodeName(), ImmutableList.toImmutableList()))
                .entrySet().stream()
                //Build Map
                //  Key: Node Name
                //  Value: AggregatedRequestedResourcesPerNode
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> aggregateRequestedResourcesPerNode(e.getValue())
                        )
                );

        var instance = new ClusterRequestResources();
        instance.requestedResourceMap = map;

        return instance;
    }

    public BigDecimal getRequestedResources(@Nonnull Node node, @Nonnull String resourceName) {
        var nodeName = node.getMetadata().getName();
        var nodesRequestedResources = requestedResourceMap.get(nodeName);
        if (nodesRequestedResources == null) {
            throw new RuntimeException(
                    MessageFormat.format("Node {} is not part of the RequestedResourceMap", nodeName)
            );
        }

        return nodesRequestedResources.getOrDefault(resourceName, BigDecimal.valueOf(0));
    }
}
