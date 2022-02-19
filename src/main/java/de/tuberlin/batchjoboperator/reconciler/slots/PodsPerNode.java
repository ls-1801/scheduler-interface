package de.tuberlin.batchjoboperator.reconciler.slots;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_IS_GHOSTPOD_NAME;

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

        var map = pods.stream()
                      .map(ApplicationPodView::wrap)
                      .filter(pod -> pod.getSlotId() != null)
                      .sorted(Comparator.comparingInt(ApplicationPodView::getSlotId))
                      .collect(Collectors.groupingBy(pod -> pod.getSpec().getNodeName(),
                              toImmutableList()));
        return new PodsPerNode(ImmutableMap.copyOf(map));
    }

    public PodsPerNode diff(@Nonnull PodsPerNode other) {

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

                              return Sets.difference(thisPodSet, otherPodSet).stream()
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

    @Nonnull
    public List<Pair<String, ? extends Pod>> getPodNodePairs() {
        return this.podsPerNodeMap.entrySet().stream()
                                  .flatMap(e -> e.getValue().stream().map(pod -> Pair.of(e.getKey(), pod)))
                                  .collect(ImmutableList.toImmutableList());
    }

    @Nonnull
    public List<? extends Pod> podsOnNode(@Nonnull Node node) {
        var nodeName = node.getMetadata().getName();
        return podsPerNodeMap.getOrDefault(nodeName, ImmutableList.of());
    }

    @Nonnull
    public PodsPerNode preemptEmptySlots() {
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
                                                           .filter(this::isGhostPod)
                                                           .collect(toImmutableList());
                                      }));

        return new PodsPerNode(preemptPodsMap);
    }

    private boolean isGhostPod(Pod pod) {
        var isGhostPod =
                pod.getMetadata().getLabels()
                   .getOrDefault(SLOT_POD_IS_GHOSTPOD_NAME, "false");

        return Boolean.parseBoolean(isGhostPod);
    }

    @Getter
    private static class ComparePodByNameWrapper {
        transient private final ApplicationPodView pod;


        public ComparePodByNameWrapper(ApplicationPodView pod) {
            this.pod = pod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComparePodByNameWrapper that = (ComparePodByNameWrapper) o;
            return Objects.equals(pod.getSlotId(), that.pod.getSlotId()) && pod.getRequestMap()
                                                                               .equals(that.pod.getRequestMap());
        }

        @Override
        public int hashCode() {
            return Objects.hash(pod.getRequestMap(), pod.getSlotId());
        }
    }
}
