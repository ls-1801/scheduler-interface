package de.tuberlin.batchjoboperator.testbedreconciler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

public class PodsPerNode {
    @Nonnull
    private final ImmutableMap<String, ImmutableList<ApplicationPodView>> podsPerNodeMap;

    private PodsPerNode(@Nonnull ImmutableMap<String, ImmutableList<ApplicationPodView>> podsPerNodeMap) {
        this.podsPerNodeMap = podsPerNodeMap;
    }

    static PodsPerNode groupByNode(PodList pods) {
        return groupByNode(pods.getItems());
    }


    static PodsPerNode groupByNode(List<Pod> pods) {
        if (pods.stream().anyMatch(pod -> pod.getSpec().getNodeName() == null)) {
            throw new SchedulingInProgressException("Pod is not scheduled yet, abort loop");
        }

        if (pods.stream().anyMatch(pod -> pod.getMetadata().getDeletionTimestamp() != null)) {
            throw new SchedulingInProgressException("Pod is being deleted, abort loop");
        }

        var map = pods.stream()
                      .map(ApplicationPodView::wrap)
                      .filter(pod -> pod.getSlotId() != null)
                      .sorted(Comparator.comparingInt(ApplicationPodView::getSlotId))
                      .collect(Collectors.groupingBy(pod -> pod.getSpec().getNodeName(),
                              toImmutableList()));
        return new PodsPerNode(ImmutableMap.copyOf(map));
    }

    public PodsPerNode diff(@Nonnull PodsPerNode other) {
        return diff(other, false);
    }

    /**
     * The returned PodsPerNodes contains all Pods that are contained by this instance and not contained the other
     * instance. Other Instance may also contain elements not present in this instance; these are simply ignored.
     * Example:   {n1: [p1, p2], n2: [p3, p4]}.diff({n1: [p1], n2: [p3]})         -> {n1: [p2], n2: [p4]}
     * But:       {n1: [p1], n2: [p3]}        .diff({n1: [p1, p2], n2: [p3, p4]}) -> {n1: [], n2: []}
     * <p>
     * The compareGhostPodFlag is used if two pods are not considered equal if one of them is a ghost pod while the
     * other is not. Preemption needs to differentiate between ghost and non ghost pods.
     */
    private PodsPerNode diff(@Nonnull PodsPerNode other, boolean compareGhostPodFlag) {

        var diffMap = Sets.union(podsPerNodeMap.keySet(), other.podsPerNodeMap.keySet())
                          .stream()
                          .collect(ImmutableMap.toImmutableMap(Function.identity(), key -> {
                              var thisPodSet =
                                      this.podsPerNodeMap.getOrDefault(key, ImmutableList.of())
                                                         .stream()
                                                         .map(pod -> new ComparePodByNameWrapper(pod,
                                                                 compareGhostPodFlag))
                                                         .collect(ImmutableSet.toImmutableSet());

                              var otherPodSet =
                                      other.podsPerNodeMap.getOrDefault(key, ImmutableList.of())
                                                          .stream()
                                                          .map(pod -> new ComparePodByNameWrapper(pod,
                                                                  compareGhostPodFlag))
                                                          .collect(ImmutableSet.toImmutableSet());

                              return Sets.difference(thisPodSet, otherPodSet).stream()
                                         .map(ComparePodByNameWrapper::getPod)
                                         .sorted(Comparator.comparingInt(ApplicationPodView::getSlotId))
                                         .collect(ImmutableList.toImmutableList());
                          }));
        return new PodsPerNode(diffMap.entrySet().stream()
                                      .filter(e -> !e.getValue().isEmpty())
                                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public PodsPerNode union(@Nonnull PodsPerNode other) {

        var diffMap = Sets.union(podsPerNodeMap.keySet(), other.podsPerNodeMap.keySet())
                          .stream()
                          .collect(ImmutableMap.toImmutableMap(Function.identity(), key -> {
                              var thisPodSet =
                                      this.podsPerNodeMap.getOrDefault(key, ImmutableList.of())
                                                         .stream().map(ComparePodByNameWrapper::new)
                                                         .collect(ImmutableSet.toImmutableSet());

                              var otherPodSet =
                                      other.podsPerNodeMap.getOrDefault(key, ImmutableList.of())
                                                          .stream().map(ComparePodByNameWrapper::new)
                                                          .collect(ImmutableSet.toImmutableSet());

                              return Sets.union(thisPodSet, otherPodSet).stream()
                                         .map(ComparePodByNameWrapper::getPod)
                                         .sorted(Comparator.comparingInt(ApplicationPodView::getSlotId))
                                         .collect(ImmutableList.toImmutableList());
                          }));
        return new PodsPerNode(diffMap.entrySet().stream()
                                      .filter(e -> !e.getValue().isEmpty())
                                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Nonnull
    public List<ApplicationPodView> getPods() {
        return this.podsPerNodeMap.values().stream()
                                  .flatMap(ImmutableList::stream)
                                  .collect(toImmutableList());
    }

    public PodsPerNode getCompletedPods() {
        return PodsPerNode.groupByNode(podsPerNodeMap.values().stream().flatMap(Collection::stream)
                                                     .filter(pod -> getNullSafe(() -> "Succeeded".equals(pod.getStatus()
                                                                                                            .getPhase())).orElse(false))
                                                     .collect(Collectors.toList()));
    }

    public PodsPerNode getPodsWithProblems() {
        return PodsPerNode.groupByNode(podsPerNodeMap.values().stream().flatMap(Collection::stream)
                                                     .filter(pod -> getNullSafe(() -> "Failed".equals(pod.getStatus()
                                                                                                         .getPhase())).orElse(false))
                                                     .collect(Collectors.toList()));
    }

    @Nonnull
    public PodsPerNode getPreemptedSlots() {
        var preemptPodsMap =
                podsPerNodeMap.entrySet().stream()
                              .collect(ImmutableMap.toImmutableMap(
                                      Map.Entry::getKey,
                                      e -> {
                                          var podList = e.getValue();
                                          var podByIdMap = podList.stream().collect(
                                                  Collectors.groupingBy(ApplicationPodView::getSlotId,
                                                          Collectors.toList())
                                          );

                                          return podByIdMap.values().stream()
                                                           .filter(list -> list.size() > 1)
                                                           .flatMap(Collection::stream)
                                                           .filter(ApplicationPodView::isGhostPod)
                                                           .collect(toImmutableList());
                                      }));

        return new PodsPerNode(preemptPodsMap);
    }

    @Nonnull
    public PodsPerNode removePreemptedSlots(PodsPerNode other) {
        return diff(other, true);
    }


    public PodsPerNode removePodsWithProblems(PodsPerNode podsWithProblems) {
        return diff(podsWithProblems, true);
    }

    public PodsPerNode removeCompletedPods(PodsPerNode completedPods) {
        return diff(completedPods, true);
    }
}
