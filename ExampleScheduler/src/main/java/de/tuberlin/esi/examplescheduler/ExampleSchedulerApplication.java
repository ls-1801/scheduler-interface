package de.tuberlin.esi.examplescheduler;


import de.tuberlin.esi.common.crd.batchjob.ScheduledEvents;
import de.tuberlin.esi.schedulingreconciler.external.ExternalBatchJob;
import de.tuberlin.esi.schedulingreconciler.external.ExternalScheduling;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@Slf4j
@CommandLine.Command(name = "Example Scheduler", version = "0.0.1-SNAPSHOT", mixinStandardHelpOptions = true)
public class ExampleSchedulerApplication implements Runnable {

    @CommandLine.Option(names = "-p", description = "Profiler Testbed name", defaultValue = "profiler-slots")
    String profilerTestbedName;

    @CommandLine.Option(names = "-s", description = "Scheduler Testbed name", defaultValue = "scheduler-slots")
    String schedulerTestbedName;

    @CommandLine.Parameters(description = "External Scheduler URL")
    URL externalInterfaceUrl;

    @CommandLine.Option(names = "--just-print", description = "Application will not run but only print the matrix",
            defaultValue = "false")
    Boolean justPrint;

    private ExternalSchedulerInterface externalSchedulerInterface;

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        log.info("args: {}", Arrays.stream(args).collect(Collectors.toList()));
        new CommandLine(new ExampleSchedulerApplication()).execute(args);
    }

    @SneakyThrows
    @Override
    public void run() {
        this.externalSchedulerInterface = new ExternalSchedulerInterface(externalInterfaceUrl);

        if (BooleanUtils.isTrue(justPrint)) {
            var jobs = externalSchedulerInterface.getJobs();
            var matrix = JobRuntimeMatrix.buildMatrix(jobs.getValue().values());
            matrix.print();
            return;
        }

        var executor = Executors.newFixedThreadPool(2);
        var profiler = executor.submit(this::profilerApplication);
        var scheduler = executor.submit(this::schedulerApplication);

        profiler.get();
        scheduler.get();
    }

    void profilerApplication() {
        try {
            profiler();
        } catch (Exception e) {
            log.error("Profiler encountered an error", e);
        }
    }

    void schedulerApplication() {
        try {
            scheduler();
        } catch (Exception e) {
            log.error("Scheduler encountered an error", e);
        }
    }

    private ScheduledEvents getLatestEvent(ExternalBatchJob job) {
        assert job != null;
        var lastEventJob =
                CollectionUtils.lastElement(job.getScheduledEvents());
        if (lastEventJob == null || BooleanUtils.isNotTrue(lastEventJob.getSuccessful())) {
            log.warn("BatchJob execution was not Successful for job: {}", job.getName());
        }
        return lastEventJob;

    }

    @SneakyThrows
    void profiler() {
        int iterations = 0;
        while (true) {
            var jobs = externalSchedulerInterface.getJobs();
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
                                               .testBed(profilerTestbedName)
                                               .build();
            try {
                externalSchedulerInterface.waitForTestbedToBecomeReady(profilerTestbedName);
                externalSchedulerInterface.createScheduling(scheduling);
                externalSchedulerInterface.waitForSchedulingToComplete(schedulingName);
            } catch (RuntimeException runtimeException) {
                continue;
            }

            var updatedJobs = externalSchedulerInterface.getJobs();

            var firstJobLatestEvent = getLatestEvent(updatedJobs.getValue().get(pairing.getKey()));
            matrix.updatePairing(pairing.getKey(), pairing.getValue(), firstJobLatestEvent);
            externalSchedulerInterface.updateJob(matrix.updateBatchJob(updatedJobs.getValue().get(pairing.getKey())));

            var secondJobLatestEvent = getLatestEvent(updatedJobs.getValue().get(pairing.getValue()));
            matrix.updatePairing(pairing.getValue(), pairing.getKey(), secondJobLatestEvent);
            externalSchedulerInterface.updateJob(matrix.updateBatchJob(updatedJobs.getValue().get(pairing.getValue())));

            externalSchedulerInterface.deleteScheduling(scheduling);
            Thread.sleep(3000);
            iterations++;
        }

    }


    @SneakyThrows
    void scheduler() {
        while (true) {
            Thread.sleep(5000);
            var jobs = externalSchedulerInterface.getJobs();
            var matrix = JobRuntimeMatrix.buildMatrix(jobs.getValue().values());
            matrix.print();

            var testbed = externalSchedulerInterface.getTestbed(schedulerTestbedName).block();
            assert testbed != null;

            var numberOfNodes = testbed.getNumberOfNodes();
            var numberOfSlotsPerNode = testbed.getNumberOfSlotsPerNode();

            // Scheduler is only implemented for 2 slots per node
            assert numberOfSlotsPerNode == 2;

            var numberOfSlots = numberOfSlotsPerNode * numberOfSlotsPerNode;

            // Pick a Job for Every Node
            var numberOfJobs = numberOfNodes;

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

            //second pass
            var availableJobs = new HashSet<>(jobNames);
            for (int i = 0; i < numberOfSlots / numberOfSlotsPerNode; i++) {
                var jobName = queue.get(i);
                var possibleCoLocation = matrix.findCoLocationWithTheLeastRuntime(jobName)
                                               .filter(availableJobs::contains)
                                               .findFirst();
                var coLocation = possibleCoLocation.orElse(CollectionUtils.firstElement(availableJobs));
                //prevent job from beeing picked more than once
                availableJobs.remove(coLocation);
                queue.add(coLocation);
            }

            var scheduling = ExternalScheduling.builder()
                                               .name("example-scheduling")
                                               .queue(queue)
                                               .testBed(schedulerTestbedName)
                                               .build();

            log.info("Calculated Scheduling: {}", queue);

            try {
                externalSchedulerInterface.waitForTestbedToBecomeReady(schedulerTestbedName);
                externalSchedulerInterface.createScheduling(scheduling);
                externalSchedulerInterface.waitForSchedulingToComplete("example-scheduling");
            } catch (RuntimeException runtimeException) {
                continue;
            }

            externalSchedulerInterface.deleteScheduling(scheduling);
        }
    }


}
