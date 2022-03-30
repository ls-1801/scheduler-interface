package de.tuberlin.esi.examplescheduler;

import de.tuberlin.esi.common.crd.scheduling.SchedulingState;
import de.tuberlin.esi.common.crd.testbed.TestbedState;
import de.tuberlin.esi.schedulingreconciler.external.ExternalBatchJob;
import de.tuberlin.esi.schedulingreconciler.external.ExternalResourceModification;
import de.tuberlin.esi.schedulingreconciler.external.ExternalScheduling;
import de.tuberlin.esi.schedulingreconciler.external.ExternalTestbed;
import de.tuberlin.esi.schedulingreconciler.external.JobsByName;
import io.fabric8.kubernetes.client.Watcher;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.function.Function;

@Slf4j
public class ExternalSchedulerInterface {

    private final WebClient client;
    private final String stompUrl;

    public ExternalSchedulerInterface(URL externalInterfaceUrl) {
        this.client = WebClient.builder()
                               .baseUrl(externalInterfaceUrl.toString())
                               .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                       MediaType.APPLICATION_JSON_VALUE)
                               .build();

        this.stompUrl = "ws://" + externalInterfaceUrl.getHost() + ":" + externalInterfaceUrl.getPort() + "/scheduler";
    }

    private <V> Function<ClientResponse, Mono<V>> toMono(Class<V> clazz) {
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
    public JobsByName getJobs() {
        return client.get()
                     .uri("/external/jobs")
                     .exchangeToMono(toMono(JobsByName.class))
                     .retry(3)
                     .block();

    }

    @SneakyThrows
    public Mono<ExternalTestbed> getTestbed(String name) {
        return client.get()
                     .uri("/external/testbeds/" + name)
                     .exchangeToMono(toMono(ExternalTestbed.class))
                     .retry(3);
    }

    @SneakyThrows
    public Mono<ExternalScheduling> getScheduling(String name) {
        return client.get()
                     .uri("/external/schedulings/" + name)
                     .exchangeToMono(toMono(ExternalScheduling.class))
                     .retry(3);
    }

    @SneakyThrows
    public ExternalBatchJob updateJob(ExternalBatchJob batchJob) {

        return client.put()
                     .uri("/external/jobs/")
                     .body(BodyInserters.fromValue(batchJob))
                     .exchangeToMono(toMono(ExternalBatchJob.class))
                     .retry(3)
                     .block();
    }

    @SneakyThrows
    public ExternalScheduling createScheduling(ExternalScheduling scheduling) {
        return client.post()
                     .uri("/external/schedulings")
                     .body(BodyInserters.fromValue(scheduling))
                     .exchangeToMono(toMono(ExternalScheduling.class))
                     .retry(3)
                     .block();

    }

    @SneakyThrows
    public void deleteScheduling(ExternalScheduling scheduling) {
        client.delete()
              .uri("/external/schedulings/" + scheduling.getName())
              .exchangeToMono(toMono(Void.class))
              .retry(3)
              .block();

    }

    public void waitForSchedulingToComplete(String name) {
        new ExternalSchedulerStompSessionHandler<>(
                stompUrl,
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
        }.toMono().block();
    }

    public void waitForTestbedToBecomeReady(String name) {
        new ExternalSchedulerStompSessionHandler<>(
                stompUrl,
                "/topic/testbeds/" + name,
                ExternalTestbed.class
        ) {

            @Override
            protected boolean handleMessage(ExternalResourceModification<ExternalTestbed> payload) {
                if (payload.getAction() == Watcher.Action.DELETED) {
                    throw new RuntimeException("Testbed was removed");
                }

                if (payload.getAction() == Watcher.Action.MODIFIED &&
                        payload.getResource().getState() == TestbedState.SUCCESS) {
                    log.info("Testbed {} is ready", name);
                    return true;
                }
                return false;
            }

            @Override
            protected Mono<Boolean> initial() {
                return getTestbed(name).map(testbed -> testbed.getState() == TestbedState.SUCCESS);
            }
        }.toMono().block();

    }
}
