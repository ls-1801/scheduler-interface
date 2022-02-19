package de.tuberlin.batchjoboperator.reconciler.batchjob.spark;

import de.tuberlin.batchjoboperator.reconciler.batchjob.common.ApplicationManager;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.MostRecentResourceMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8s.sparkoperator.SparkApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SparkApplicationManagerService {

    private final MostRecentResourceMap<SparkApplication> mostRecentResourceMap = new MostRecentResourceMap<>();

    private final KubernetesClient client;


    public ApplicationManager getManager(SparkApplication application) {
        var old = mostRecentResourceMap.updateAndReturnOld(application);
        return new SparkApplicationManager(client, this, application, old);
    }

    public void removeManager(SparkApplication mostRecent) {
        mostRecentResourceMap.remove(mostRecent);
    }
}
