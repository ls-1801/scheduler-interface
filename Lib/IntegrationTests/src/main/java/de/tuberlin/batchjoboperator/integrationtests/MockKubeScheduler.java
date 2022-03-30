package de.tuberlin.batchjoboperator.integrationtests;

import com.google.common.collect.ImmutableSet;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.util.General;
import de.tuberlin.batchjoboperator.extender.ExtenderController;
import de.tuberlin.batchjoboperator.extender.ExtenderFilterArgs;
import de.tuberlin.batchjoboperator.extender.ExtenderPreemptionArgs;
import de.tuberlin.batchjoboperator.extender.Victims;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterAllocatableResources;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterAvailableResources;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterRequestedResources;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockKubeScheduler is used to test the Extender.
 * Based on a watcher for Pods, if we find a pod that is not scheduled (.spec.nodeName == null), the mock scheduler
 * attempts to replicate the relevant logic done by the KubeScheduler.
 * <p>
 * - Filter Nodes based on their available resources
 * - Filter Nodes based on the Pod Affinities
 * If some nodes are still possible, the mock scheduler calls the filter extender. If the filter extender returns
 * empty nodes we proceed with preemption. Otherwise, the NodeName is set on the Pod on the method returns.
 * <p>
 * If no possible nodes remains after filtering, preemption is triggered. Preemption returns a UID to a pod that is
 * deemed the preemptee. The KubeScheduler would mark the Pod as deleted, however no pod controller is listing, and
 * thus pods will be simply deleted.
 * After a Pod is deleted the method is recursed.
 */
@RequiredArgsConstructor
@Slf4j
class MockKubeScheduler {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final KubernetesClient client;
    private final ExtenderController uut;

    public void start() {
        log.info("#" .repeat(80));
        log.info("#" .repeat(29) + "MOCK SCHEDULER STARTED" + "#" .repeat(29));
        log.info("#" .repeat(80));
        new Thread(this::run).start();
    }

    private Set<String> filterByAffinities(Pod pod, Set<String> possibleNodes, Map<String, Node> nodes) {
        var nodeSelector = General.getNullSafe(() ->
                pod.getSpec().getAffinity().getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution()
                   .getNodeSelectorTerms()
        ).orElse(emptyList());

        if (nodeSelector.isEmpty())
            return possibleNodes;

        return possibleNodes.stream().filter(nodeName ->
                // affinities allow a node to be chosen, if any node selectors match
                nodeSelector.stream()
                            .anyMatch(ns -> ns.getMatchExpressions().stream()
                                              // for a node selector to match all expression must match.
                                              .allMatch((expression) -> {
                                                  var labelKey = expression.getKey();
                                                  var labelValues = new HashSet<>(expression.getValues());

                                                  Assertions.assertThat(expression.getOperator())
                                                            .as("Only In Operator is supported")
                                                            .isEqualToIgnoringCase("in");

                                                  var nodeLabels =
                                                          General.getNullSafe(() -> nodes.get(nodeName)
                                                                                         .getMetadata()
                                                                                         .getLabels())
                                                                 .orElse(emptyMap());

                                                  var labelValue = nodeLabels.get(labelKey);
                                                  return labelValues.contains(labelValue);
                                              }))
        ).collect(ImmutableSet.toImmutableSet());

    }

    private synchronized void filterNodes(Pod resource, int retries) {
        final var pod = client.pods().inNamespace(NamespacedName.getNamespace(resource))
                              .withName(NamespacedName.getName(resource)).get();
        // Synchronized prevents filterNodes from executing concurrently, however the Watcher is still running and
        // may call filterNodes with outdated pods. We Update the pod in question and recheck if it still needs
        // scheduling
        if (pod == null) {
            log.info("Pod has already been deleted. Abort");
            return;
        }

        if (pod.getSpec().getNodeName() != null) {
            log.info("Pod Is already Scheduled. Abort");
            return;
        }


        // This Method uses recursion. Recursing twice should never be required
        assertThat(retries).isLessThan(2);

        var nodes =
                client.nodes().list().getItems().stream().collect(Collectors.toMap(
                        n -> n.getMetadata().getName(),
                        Function.identity()
                ));

        var allPods = client.pods().inAnyNamespace().list().getItems().stream()
                            .filter(p -> p.getSpec().getNodeName() != null)
                            .collect(Collectors.toList());


        Set<String> possibleNodes = nodes.keySet();
        log.info("Initially possible nodes: {}", possibleNodes);

        possibleNodes = filterByResourceRequests(pod, nodes, allPods, possibleNodes);
        log.info("possible nodes after resource requirements: {}", possibleNodes);

        possibleNodes = filterByAffinities(pod, possibleNodes, nodes);
        log.info("possible nodes after pod affinity requirements: {}", possibleNodes);

        var filteredNodes = new NodeListBuilder()
                .withItems(possibleNodes.stream()
                                        .map(nodeName -> client.nodes().withName(nodeName).get())
                                        .collect(Collectors.toList()))
                .build();

        if (possibleNodes.size() > 0) {
            log.info("using extender filter");
            var filterArgs = new ExtenderFilterArgs(pod, filteredNodes, possibleNodes);
            var result = General.logAndRethrow(() -> uut.filter(filterArgs));
            log.info("Filter result: {}", result);

            var targetNodeName = General.requireFirstElement(result.getNodeNames());
            client.pods().inNamespace(pod.getMetadata().getNamespace())
                  .withName(pod.getMetadata().getName()).edit(updated -> {
                      updated.getSpec().setNodeName(targetNodeName);
                      return updated;
                  });
            client.pods().inNamespace(pod.getMetadata().getNamespace())
                  .withName(pod.getMetadata().getName()).editStatus(updated -> {
                      if (updated.getStatus() == null) {
                          updated.setStatus(new PodStatus());
                      }

                      updated.getStatus().setPhase("Running");

                      return updated;
                  });
            log.info("scheduled pod to node: {}", targetNodeName);

            return;
        }

        log.info("using extender preemption");
        // To Lazy for proper arguments the Extender does not use them
        var map = nodes.keySet().stream()
                       .collect(Collectors.toMap(Function.identity(), (n) -> new Victims(emptyList(), 0L)));
        var preemptionArgs = new ExtenderPreemptionArgs(pod, map, Collections.emptyMap());
        var result = General.logAndRethrow(() -> uut.preemption(preemptionArgs));

        log.info("preemption result: {}", result);
        var victimPodUid = General.requireFirstElement(result.getNodeNameToMetaVictims().entrySet())
                                  .getValue().getPods().get(0).getUid();

        client.pods().inAnyNamespace().list().getItems().stream()
              .filter(p -> victimPodUid.equals(p.getMetadata().getUid()))
              .findFirst()
              .ifPresent(victimPod -> {
                  client.pods().inNamespace(victimPod.getMetadata().getNamespace()).delete(victimPod);
              });

        filterNodes(pod, retries + 1);
    }

    private Set<String> filterByResourceRequests(Pod pod, Map<String, Node> nodes, List<Pod> allPods,
                                                 Set<String> possibleNodes) {


        // Allocatable Resources for all possible Nodes
        var allocatable = ClusterAllocatableResources.aggregate(
                possibleNodes.stream().map(nodes::get).collect(Collectors.toSet())
        );

        // Aggregates Requested Resources for all Pods on possible Nodes.
        var requested = ClusterRequestedResources.aggregate(
                allPods.stream().filter(p -> possibleNodes.contains(p.getSpec().getNodeName()))
                       .collect(Collectors.toList())
        );

        // Calculate Available Resources
        var available = ClusterAvailableResources.diff(allocatable, requested);

        var resourceMap = General.getNullSafe(() ->
                                         ClusterRequestedResources.aggregateRequestedResourcesPerNode(Collections.singletonList(pod)))
                                 .orElse(Map.of());

        return available.nodesWithEnoughResources(resourceMap);
    }


    @SneakyThrows
    private void run() {
        var podWatch = client.pods().inAnyNamespace().watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod resource) {
                if (!(action.equals(Action.MODIFIED) || action.equals(Action.ADDED))) return;
                if (resource.getSpec().getNodeName() != null) return;

                log.info("Pod {} requires scheduling", NamespacedName.getName(resource));
                filterNodes(resource, 0);
            }

            @Override
            public void onClose(WatcherException cause) {

            }
        });


        latch.await();
        podWatch.close();
    }

    public void stop() {
        latch.countDown();
    }
}
