package de.tuberlin.batchjoboperator.reconciler.batchjob;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.ApplicationManager;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.NoApplicationManager;
import de.tuberlin.batchjoboperator.reconciler.batchjob.spark.SparkApplicationManagerService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import io.k8s.flinkoperator.FlinkCluster;
import io.k8s.sparkoperator.SparkApplication;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_VALUE;

/**
 * A very simple sample controller that creates a service with a label.
 */
@ControllerConfiguration
@RequiredArgsConstructor
public class BatchJobReconciler implements Reconciler<BatchJob>, EventSourceInitializer<BatchJob> {

    private static final Logger log = LoggerFactory.getLogger(BatchJobReconciler.class);

    private final KubernetesClient kubernetesClient;

    private final SparkApplicationManagerService sparkApplicationManagerService;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BatchJob> context) {
        SharedIndexInformer<SparkApplication> sparkApplicationSharedIndexInformer =
                kubernetesClient.resources(SparkApplication.class)
                                .inAnyNamespace()
                                .withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);
        SharedIndexInformer<FlinkCluster> flinkApplicationSharedIndexInformer =
                kubernetesClient.resources(FlinkCluster.class)
                                .inAnyNamespace()
                                //.withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);

        return List.of(new InformerEventSource<>(sparkApplicationSharedIndexInformer, Mappers.fromOwnerReference())
                , new InformerEventSource<>(flinkApplicationSharedIndexInformer, (cluster) -> {
                    log.info("Flink Cluster found: {}", cluster);
                    return Collections.emptySet();
                }));
    }

    @Override
    public DeleteControl cleanup(BatchJob resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        return Reconciler.super.cleanup(resource, context);
    }


    @Override
    public UpdateControl<BatchJob> reconcile(BatchJob resource, Context context) {
        log.info("Reconciling: {}", resource.getMetadata().getName());

        var manager = context.getSecondaryResource(SparkApplication.class).map(this::getSparkApplicationManager)
                             .or(() -> context.getSecondaryResource(FlinkCluster.class)
                                              .map(this::getFlinkApplicationManager))
                             .orElseGet(this::handleNoApplication);

        return manager.handle(resource);
    }

    private ApplicationManager handleNoApplication() {
        return new NoApplicationManager();
    }

    private ApplicationManager getSparkApplicationManager(SparkApplication sparkApp) {
        return sparkApplicationManagerService.getManager(sparkApp);
    }

    private ApplicationManager getFlinkApplicationManager(FlinkCluster sparkApp) {
        throw new RuntimeException("Not Implemented");
    }
}