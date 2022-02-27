package de.tuberlin.batchjoboperator.reconciler.batchjob.flink;

import de.tuberlin.batchjoboperator.reconciler.batchjob.common.ApplicationManager;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.MostRecentResourceMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8s.flinkoperator.FlinkCluster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FlinkApplicationManagerService {

    private final MostRecentResourceMap<FlinkCluster> mostRecentResourceMap = new MostRecentResourceMap<>();

    private final KubernetesClient client;


    public ApplicationManager getManager(FlinkCluster application) {
        var old = mostRecentResourceMap.updateIfNewerAndReturnOld(application);
        return new FlinkApplicationManager(client, this, application, old);
    }

    public void removeManager(FlinkCluster mostRecent) {
        mostRecentResourceMap.remove(mostRecent);
    }
}
