package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final KubernetesClient client;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ExternalMapper mapper;

    @PostConstruct
    void initializeWatches() {
        client.resources(Slot.class).inNamespace(getNamespace()).withName("batchjob-slots").watch(
                new Watcher<Slot>() {
                    @SneakyThrows
                    @Override
                    public void eventReceived(Action action, Slot resource) {
                        log.info("Slot Event: {}", action);
                        var update = new SlotsByNode(resource.getStatus().getSlots());
                        simpMessagingTemplate.convertAndSend("/topic/slots", update);
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        log.debug("Closing");
                    }
                }
        );

        client.resources(Scheduling.class).inNamespace(getNamespace()).watch(
                new Watcher<Scheduling>() {
                    @SneakyThrows
                    @Override
                    public void eventReceived(Action action, Scheduling resource) {
                        log.info("Scheduling Event: {}", action);
                        if (Action.DELETED.equals(action)) return;

                        var update = mapper.toExternal(resource);
                        simpMessagingTemplate.convertAndSend("/topic/scheduling", update);
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        log.debug("Closing");
                    }
                }
        );


        client.resources(BatchJob.class).inNamespace(getNamespace()).watch(
                new Watcher<BatchJob>() {
                    @SneakyThrows
                    @Override
                    public void eventReceived(Action action, BatchJob resource) {
                        log.info("BatchJob Event: {}", action);
                        var external = mapper.toExternal(resource);
                        simpMessagingTemplate.convertAndSend("/topic/jobs", external);
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        log.debug("Closing");
                    }
                }
        );
    }


    @GetMapping("/external/slots")
    public SlotsByNode getSlots() {
        var slots = client.resources(Slot.class).inNamespace(getNamespace()).withName("batchjob-slots").get();


        return new SlotsByNode(slots.getStatus().getSlots());
    }

    @GetMapping("/external/jobs")
    public JobsByName getJobs() {
        var jobs = client.resources(BatchJob.class).inNamespace(getNamespace()).list();

        return new JobsByName(jobs.getItems().stream().map(mapper::toExternal).collect(Collectors.toList()));
    }

    @GetMapping("/external/scheduling")
    public ExternalScheduling getScheduling() {
        var schedulings = client.resources(Scheduling.class).inNamespace(getNamespace()).list();
        if (schedulings.getItems().isEmpty())
            throw new ResourceNotFoundException("Scheduling");

        return mapper.toExternal(schedulings.getItems().get(0));
    }

    @DeleteMapping("/external/scheduling/{schedulingName}")
    public ExternalScheduling restartScheduling(@PathVariable("schedulingName") String schedulingName) {
        var oldScheduling = client.resources(Scheduling.class).inNamespace(getNamespace())
                                  .withName(schedulingName).get();

        if (oldScheduling == null) {
            throw new ResponseStatusException(HttpStatus.NOT_MODIFIED);
        }

        var deleted = client.resources(Scheduling.class).inNamespace(getNamespace()).withName(schedulingName).delete();

        if (BooleanUtils.isNotTrue(deleted))
            throw new ActualProblemException("Could not delete the scheduling");


        return mapper.toExternal(oldScheduling);
    }

    @PostMapping("/external/scheduling/{schedulingName}")
    public ExternalScheduling createScheduling(
            @RequestBody ExternalScheduling scheduling,
            @PathVariable("schedulingName") String schedulingName
    ) {

        var newScheduling = mapper.toInternal(scheduling, new NamespacedName("batchjob-slots", getNamespace()),
                getNamespace());

        client.resources(Scheduling.class).inNamespace(getNamespace()).withName(schedulingName)
              .createOrReplace(newScheduling);

        return mapper.toExternal(newScheduling);
    }

    private String getNamespace() {
        return "default";
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
