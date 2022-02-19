package io.k8s.flinkoperator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;


@Group("flinkoperator.k8s.io")
@Version("v1beta1")
@Singular("flinkcluster")
@Kind("FlinkCluster")
public class FlinkCluster extends CustomResource<V1beta1FlinkClusterSpec, V1beta1FlinkClusterStatus> implements Namespaced {

}
