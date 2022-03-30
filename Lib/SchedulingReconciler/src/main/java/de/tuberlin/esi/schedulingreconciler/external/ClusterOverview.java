package de.tuberlin.esi.schedulingreconciler.external;

import io.fabric8.kubernetes.api.model.Namespaced;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ClusterOverview {
    Map<String, List<Namespaced>> nodes;
}

