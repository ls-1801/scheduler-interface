package k8s.sparkoperator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("sparkoperator.k8s.io")
@Version("v1beta2")
@ShortNames("sparkapp")
public class SparkApplication extends CustomResource<V1beta2SparkApplicationSpec, V1beta2SparkApplicationStatus> implements Namespaced {

}
