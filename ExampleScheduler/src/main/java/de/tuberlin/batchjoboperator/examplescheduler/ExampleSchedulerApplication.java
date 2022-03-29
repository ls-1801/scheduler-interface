package de.tuberlin.batchjoboperator.examplescheduler;


import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalBatchJob;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalResourceModification;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalScheduling;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalTestbed;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.JobsByName;
import io.fabric8.kubernetes.client.Watcher;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class ExampleSchedulerApplication {

    private static final String HOST_NAME = "localhost";
    private static final int PORT = 8082;
    private static final String HTTP_URL = "http://" + HOST_NAME + ":" + PORT;
    private static final WebClient client = WebClient.builder()
                                                     .baseUrl(HTTP_URL)
                                                     .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                             MediaType.APPLICATION_JSON_VALUE)
                                                     .build();
    private static final String STOMP_URL = "ws://" + HOST_NAME + ":" + PORT + "/scheduler";

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        var executor = Executors.newFixedThreadPool(2);
        var profiler = executor.submit(ExampleSchedulerApplication::profilerApplication);
        var scheduler = executor.submit(ExampleSchedulerApplication::schedulerApplication);

        profiler.get();
        scheduler.get();

    }

    static void profilerApplication() {
        try {
            profiler();
        } catch (Exception e) {
            log.error("Profiler encountered an error", e);
        }
    }

    static void schedulerApplication() {
        try {
            scheduler();
        } catch (Exception e) {
            log.error("Scheduler encountered an error", e);
        }
    }

    private static <V> Function<ClientResponse, Mono<V>> toMono(Class<V> clazz) {
        return response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(clazz);
            }
            else {
                log.error("Got response code: {}", response.statusCode());
                return response.createException()
                               .flatMap(Mono::error);
            }
        };
    }


    @SneakyThrows
    private static JobsByName getJobs() {
        return client.get()
                     .uri("/external/jobs")
                     .exchangeToMono(toMono(JobsByName.class))
                     .retry(3)
                     .block();

    }

    @SneakyThrows
    private static Mono<ExternalTestbed> getTestbed(String name) {
        return client.get()
                     .uri("/external/testbeds/" + name)
                     .exchangeToMono(toMono(ExternalTestbed.class))
                     .retry(3);
    }

    @SneakyThrows
    private static Mono<ExternalScheduling> getScheduling(String name) {
        return client.get()
                     .uri("/external/schedulings/" + name)
                     .exchangeToMono(toMono(ExternalScheduling.class))
                     .retry(3);
    }


    @SneakyThrows
    private static ExternalBatchJob updateJob(ExternalBatchJob batchJob) {

        return client.put()
                     .uri("/external/jobs/")
                     .body(BodyInserters.fromValue(batchJob))
                     .exchangeToMono(toMono(ExternalBatchJob.class))
                     .retry(3)
                     .block();
    }

    @SneakyThrows
    private static ExternalScheduling createScheduling(ExternalScheduling scheduling) {
        return client.post()
                     .uri("/external/schedulings")
                     .body(BodyInserters.fromValue(scheduling))
                     .exchangeToMono(toMono(ExternalScheduling.class))
                     .retry(3)
                     .block();

    }

    @SneakyThrows
    private static Void deleteScheduling(ExternalScheduling scheduling) {
        return client.delete()
                     .uri("/external/schedulings/" + scheduling.getName())
                     .exchangeToMono(toMono(Void.class))
                     .retry(3)
                     .block();

    }

    private static Mono<Void> waitForSchedulingToComplete(String name) {
        return new ExternalSchedulerStompSessionHandler<>(
                STOMP_URL,
                "/topic/schedulings/" + name,
                ExternalScheduling.class
        ) {

            @Override
            protected boolean handleMessage(ExternalResourceModification<ExternalScheduling> payload) {
                if (payload.getAction() == Watcher.Action.DELETED) {
                    throw new RuntimeException("Scheduling " + name + " was deleted");
                }

                if (payload.getResource().getState() == SchedulingState.CompletedState) {
                    log.info("Scheduling {} is completed", name);
                    return true;
                }

                return false;
            }

            @Override
            protected Mono<Boolean> initial() {
                return getScheduling(name).map(scheduling -> {
                    return scheduling.getState() == SchedulingState.CompletedState;
                });
            }
        }.toMono();
    }

    private static Mono<Void> waitForTestbedToBecomeReady(String name) {

        return new ExternalSchedulerStompSessionHandler<>(
                STOMP_URL,
                "/topic/testbeds/" + name,
                ExternalTestbed.class
        ) {

            @Override
            protected boolean handleMessage(ExternalResourceModification<ExternalTestbed> payload) {
                if (payload.getAction() == Watcher.Action.DELETED) {
                    throw new RuntimeException("Testbed was removed");
                }

                if (payload.getAction() == Watcher.Action.MODIFIED &&
                        payload.getResource().getState() == SlotsStatusState.SUCCESS) {
                    log.info("Testbed {} is ready", name);
                    return true;
                }
                return false;
            }

            @Override
            protected Mono<Boolean> initial() {
                return getTestbed(name).map(testbed -> testbed.getState() == SlotsStatusState.SUCCESS);
            }
        }.toMono();

    }

    @SneakyThrows
    static void profiler() {
        int iterations = 0;
        while (true) {
            var jobs = getJobs();
            var matrix = JobRuntimeMatrix.buildMatrix(jobs.getValue().values());

            var pairingOpt = matrix.findPairingWithTheLeastSamples(jobs.getValue().keySet());

            if (pairingOpt.isEmpty()) {
                log.info("All possible co-locations tested");
                return;
            }

            var pairing = pairingOpt.get();

            log.info("Pairing: {}", pairing);
            var schedulingName = "profiler-scheduling-" + iterations;
            var scheduling = ExternalScheduling.builder()
                                               .name(schedulingName)
                                               .queue(List.of(pairing.getKey(), pairing.getValue()))
                                               .testBed("profiler-slots")
                                               .build();
            try {
                waitForTestbedToBecomeReady("profiler-slots").block();
                createScheduling(scheduling);
                waitForSchedulingToComplete(schedulingName).block();
            } catch (RuntimeException runtimeException) {
                continue;
            }

            var updatedJobs = getJobs();

            var lastEventFirstJob =
                    CollectionUtils.lastElement(updatedJobs.getValue().get(pairing.getKey()).getScheduledEvents());
            if (lastEventFirstJob == null || BooleanUtils.isNotTrue(lastEventFirstJob.getSuccessful())) {
                log.warn("BatchJob execution was not Successful for job: {}", pairing.getKey());
            }

            var lastEventSecondJob =
                    CollectionUtils.lastElement(updatedJobs.getValue().get(pairing.getValue())
                                                           .getScheduledEvents());
            if (lastEventSecondJob == null || BooleanUtils.isNotTrue(lastEventSecondJob.getSuccessful())) {
                log.warn("BatchJob execution was not Successful for job: {}", pairing.getKey());
            }

            matrix.updatePairing(pairing.getKey(), pairing.getValue(), lastEventFirstJob);
            matrix.updatePairing(pairing.getValue(), pairing.getKey(), lastEventSecondJob);

            updateJob(matrix.updateBatchJob(updatedJobs.getValue().get(pairing.getKey())));
            updateJob(matrix.updateBatchJob(updatedJobs.getValue().get(pairing.getValue())));

            deleteScheduling(scheduling);
            Thread.sleep(3000);
            iterations++;
        }

    }


    @SneakyThrows
    static void scheduler() {
        while (true) {
            Thread.sleep(5000);
            var jobs = getJobs();
            var matrix = JobRuntimeMatrix.buildMatrix(jobs.getValue().values());
            matrix.print();

            var numberOfSlots = 4;
            var numberOfSlotsPerNode = 2;
            var numberOfJobs = numberOfSlots / numberOfSlotsPerNode;
            assert jobs.getValue().size() > numberOfJobs;

            var allJobs = new ArrayList<>(jobs.getValue().values());
            Collections.shuffle(allJobs);
            var jobNames = allJobs.stream().limit(numberOfJobs)
                                  .map(ExternalBatchJob::getName)
                                  .collect(Collectors.toList());

            log.info("Finding Scheduling for Jobs: {}", jobNames);

            var queue = new ArrayList<String>();
            // First pass
            for (int i = 0; i < numberOfSlots / numberOfSlotsPerNode; i++) {
                var jobName = jobNames.get(i % jobNames.size());
                queue.add(jobName);
            }

            var availableJobs = new HashSet<>(jobNames);
            for (int i = 0; i < numberOfSlots / numberOfSlotsPerNode; i++) {
                var jobName = queue.get(i);
                var possibleCoLocation = matrix.findCoLocationWithTheLeastRuntime(jobName)
                                               .filter(availableJobs::contains)
                                               .findFirst();
                var coLocation = possibleCoLocation.orElse(CollectionUtils.firstElement(availableJobs));
                availableJobs.remove(coLocation);
                queue.add(coLocation);
            }

            var scheduling = ExternalScheduling.builder()
                                               .name("example-scheduling")
                                               .queue(queue)
                                               .testBed("batchjob-slots")
                                               .build();

            log.info("Calculated Scheduling: {}", queue);

            try {
                waitForTestbedToBecomeReady("batchjob-slots").block();
                createScheduling(scheduling);
                waitForSchedulingToComplete("example-scheduling").block();
            } catch (RuntimeException runtimeException) {
                continue;
            }

            deleteScheduling(scheduling);
        }
    }
}
