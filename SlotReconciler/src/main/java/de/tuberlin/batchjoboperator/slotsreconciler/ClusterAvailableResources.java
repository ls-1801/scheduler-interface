package de.tuberlin.batchjoboperator.slotsreconciler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.Node;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

public class ClusterAvailableResources {
    Map<String, Map<String, BigDecimal>> clusterAvailableResources;

    public static ClusterAvailableResources diff(ClusterAllocatableResources allocatableResources,
                                                 ClusterRequestedResources requestResources) {
        if (!requestResources.requestedResourceMap.keySet()
                                                  .equals(allocatableResources.allocatableResourcesMap.keySet())) {
            throw new RuntimeException("Not Supported");
        }

        var nodeBuilder = ImmutableMap.<String, Map<String, BigDecimal>>builder();
        for (var nodeName : requestResources.requestedResourceMap.keySet()) {
            var requestOnNode = requestResources.requestedResourceMap.get(nodeName);
            var allocatableOnNode = allocatableResources.allocatableResourcesMap.get(nodeName);


            var resourceBuilder = ImmutableMap.<String, BigDecimal>builder();
            for (var resourceName : allocatableOnNode.keySet()) {
                resourceBuilder.put(resourceName,
                        allocatableOnNode.get(resourceName)
                                         .subtract(requestOnNode.getOrDefault(resourceName, BigDecimal.valueOf(0))));
            }
            nodeBuilder.put(nodeName, resourceBuilder.build());
        }

        var instance = new ClusterAvailableResources();
        instance.clusterAvailableResources = nodeBuilder.build();

        return instance;
    }

    public Map<String, Map<String, BigDecimal>> getClusterAvailableResources() {
        return clusterAvailableResources;
    }

    public BigDecimal getAvailableResources(@Nonnull Node node, @Nonnull String resourceName) {
        var nodeName = node.getMetadata().getName();
        var nodesRequestedResources = clusterAvailableResources.get(nodeName);
        if (nodesRequestedResources == null) {
            throw new RuntimeException(
                    MessageFormat.format("Node {} is not part of the RequestedResourceMap", nodeName)
            );
        }

        return nodesRequestedResources.getOrDefault(resourceName, BigDecimal.valueOf(0));
    }

    public Set<String> nodesWithEnoughResources(Map<String, BigDecimal> resource) {
        return clusterAvailableResources.entrySet().stream()
                                        .filter(nodesResources -> {
                                            var resourceOnNode = nodesResources.getValue();
                                            return resource.entrySet()
                                                           .stream().allMatch(e -> {
                                                        var key = e.getKey();
                                                        var requested = e.getValue();
                                                        var available =
                                                                resourceOnNode.getOrDefault(key, BigDecimal.valueOf(0));
                                                        return available.compareTo(requested) >= 0;
                                                    });

                                        })
                                        .map(Map.Entry::getKey)
                                        .collect(ImmutableSet.toImmutableSet());
    }
}
