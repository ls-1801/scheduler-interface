package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.scheduling.Scheduling;
import de.tuberlin.esi.common.crd.slots.Slot;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.crd.NamespacedName.getName;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ExternalSchedulerInterfaceController {

    private final KubernetesClient client;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ExternalMapper mapper;

    private final String namespace;

    @PostConstruct
    void initializeWatches() {
        client.resources(Slot.class).inNamespace(namespace).watch(
                new ReportingResourceWatcher<>("testbeds", mapper::toExternal, Slot.class));

        client.resources(BatchJob.class).inNamespace(namespace).watch(
                new ReportingResourceWatcher<>("jobs", mapper::toExternal, BatchJob.class));

        client.resources(Scheduling.class).inNamespace(namespace).watch(
                new ReportingResourceWatcher<>("schedulings", mapper::toExternal, Scheduling.class));
    }

    @GetMapping("/external/testbeds")
    public List<ExternalTestbed> getTestbeds() {
        var slots = client.resources(Slot.class).inNamespace(namespace).list();

        return slots.getItems().stream()
                    .map(mapper::toExternal)
                    .collect(Collectors.toList());
    }

    @GetMapping("/external/testbeds/{name}")
    public ExternalTestbed getTestbed(@PathVariable String name) {
        var testbed = client.resources(Slot.class).inNamespace(namespace)
                            .withName(name).get();

        if (testbed == null)
            throw new ResourceNotFoundException("Testbed " + name);

        return mapper.toExternal(testbed);
    }

    @GetMapping("/external/jobs")
    public JobsByName getJobs() {
        var jobs = client.resources(BatchJob.class).inNamespace(namespace).list();

        return new JobsByName(jobs.getItems().stream().map(mapper::toExternal).collect(Collectors.toList()));
    }

    @GetMapping("/external/jobs/{name}")
    public ExternalBatchJob getJobs(@PathVariable("name") String name) {
        var job = client.resources(BatchJob.class).inNamespace(namespace)
                        .withName(name).get();

        if (job == null)
            throw new ResourceNotFoundException("Scheduling " + name);


        return mapper.toExternal(job);
    }

    @PutMapping("/external/jobs/")
    public ExternalBatchJob updateJob(@RequestBody ExternalBatchJob externalBatchJob) {
        var job = client.resources(BatchJob.class).inNamespace(namespace)
                        .withName(externalBatchJob.getName()).edit(editJob -> {
                    editJob.getSpec().setExternalScheduler(externalBatchJob.getExternalScheduler());
                    return editJob;
                });

        return mapper.toExternal(job);
    }

    @GetMapping("/external/schedulings")
    public List<ExternalScheduling> getSchedulings() {
        var schedulings = client.resources(Scheduling.class).inNamespace(namespace).list();

        return schedulings.getItems().stream().map(mapper::toExternal)
                          .collect(Collectors.toList());
    }

    @GetMapping("/external/schedulings/{schedulingName}")
    public ExternalScheduling getScheduling(@PathVariable("schedulingName") String schedulingName) {
        var scheduling = client.resources(Scheduling.class).inNamespace(namespace)
                               .withName(schedulingName).get();

        if (scheduling == null)
            throw new ResourceNotFoundException("Scheduling " + schedulingName);


        return mapper.toExternal(scheduling);
    }

    @DeleteMapping("/external/schedulings/{schedulingName}")
    public ExternalScheduling deleteScheduling(@PathVariable("schedulingName") String schedulingName) {
        var oldScheduling = client.resources(Scheduling.class).inNamespace(namespace)
                                  .withName(schedulingName).get();

        if (oldScheduling == null) {
            throw new ResponseStatusException(HttpStatus.NOT_MODIFIED);
        }

        var deleted = client.resources(Scheduling.class)
                            .inNamespace(namespace)
                            .withName(schedulingName).delete();

        if (BooleanUtils.isNotTrue(deleted))
            throw new ActualProblemException("Could not delete the scheduling");

        return mapper.toExternal(oldScheduling);
    }

    @PostMapping("/external/schedulings")
    public ExternalScheduling createScheduling(
            @RequestBody @Valid ExternalScheduling scheduling
    ) {

        var newScheduling = mapper.toInternal(scheduling, namespace);
        try {
            var updated = client.resources(Scheduling.class).inNamespace(namespace).withName(scheduling.getName())
                                .createOrReplace(newScheduling);
            return mapper.toExternal(updated);
        } catch (KubernetesClientException clientException) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, clientException.getMessage());
        }

    }

    @RequiredArgsConstructor
    private class ReportingResourceWatcher<T extends HasMetadata> implements Watcher<T> {
        private static final int MAXIMUM_RETRIES = 3;
        private final String topic;
        private final Function<T, Object> mapper;
        private final Class<T> resourceClass;
        private int retryCounter = 0;

        @Override
        public void eventReceived(Action action, T resource) {
            log.info("{} Event: {} {}", topic, getName(resource), action);
            var update = mapper.apply(resource);

            simpMessagingTemplate.convertAndSend("/topic/" + topic,
                    new ExternalResourceModification<>(action, getName(resource)));
            var modification = new ExternalResourceModification<>(action, update);
            simpMessagingTemplate.convertAndSend("/topic/" + topic + "/" + getName(resource), modification);
        }

        @Override
        public void onClose(WatcherException cause) {
            log.error("{} watch closed", topic, cause);

            if (retryCounter > MAXIMUM_RETRIES) {
                log.error("Watch does not seem to work");
                return;
            }
            try {
                // 5 seconds, 25 second, 125 seconds, 625 seconds
                Thread.sleep(1000L * (long) Math.pow(5, retryCounter));
            } catch (InterruptedException e) {
            }

            var reporter = new ReportingResourceWatcher<T>(topic, mapper, resourceClass);
            reporter.retryCounter = this.retryCounter + 1;
            client.resources(resourceClass).inNamespace(namespace).watch(reporter);
        }
    }

    class ResourceNotFoundException extends ResponseStatusException {
        public ResourceNotFoundException(String resource) {
            super(HttpStatus.NOT_FOUND, "Not found: " + resource);
        }
    }

    class ActualProblemException extends ResponseStatusException {
        public ActualProblemException(String resource) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "Problem: " + resource);
        }
    }
}
