package de.tuberlin.batchjoboperator.examplescheduler;

import com.inamik.text.tables.Cell;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.grid.Border;
import com.inamik.text.tables.grid.Util;
import de.tuberlin.batchjoboperator.common.crd.batchjob.ScheduledEvents;
import de.tuberlin.batchjoboperator.schedulingreconciler.external.ExternalBatchJob;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.inamik.text.tables.Cell.Functions.HORIZONTAL_CENTER;
import static com.inamik.text.tables.Cell.Functions.VERTICAL_CENTER;

public class JobRuntimeMatrix {
    private final HashMap<String, Map<String, JobRuntime>> matrix;

    private JobRuntimeMatrix(HashMap<String, Map<String, JobRuntime>> matrix) {
        this.matrix = matrix;
    }

    public static JobRuntimeMatrix buildMatrix(Collection<ExternalBatchJob> jobs) {
        var matrix = new HashMap<String, Map<String, JobRuntime>>();

        for (ExternalBatchJob job : jobs) {
            if (job.getExternalScheduler() == null) {
                job.setExternalScheduler(new HashMap<>());
            }

            job.getExternalScheduler().getOrDefault("profiler",
                       Collections.singletonList(Collections.emptyMap()))
               .get(0).forEach((key, value) -> {
                   matrix.computeIfAbsent(job.getName(), n -> new HashMap<>())
                         .put(key, new JobRuntime(value));
               });
        }

        return new JobRuntimeMatrix(matrix);
    }

    public void updatePairing(String jobName, String colocatedWith, ScheduledEvents event) {
        matrix.computeIfAbsent(jobName, k -> new HashMap<>())
              .computeIfAbsent(colocatedWith, k -> new JobRuntime())
              .update(event);

    }

    public Optional<Pair<String, String>> findPairingWithTheLeastSamples(Collection<String> names) {
        return names.stream().flatMap(name ->
                            names.stream().filter(coLocated -> !coLocated.equals(name)).map(coLocated -> Pair.of(name
                                    , coLocated)))
                    .sorted(Comparator.comparingLong(p -> {
                        return matrix.computeIfAbsent(p.getKey(), k -> new HashMap<>())
                                     .computeIfAbsent(p.getValue(), k -> new JobRuntime())
                                     .getNSamples();

                    }))
                    .findFirst();
    }

    public ExternalBatchJob updateBatchJob(ExternalBatchJob job) {
        this.matrix.getOrDefault(job.getName(), Collections.emptyMap()).forEach((coLocated, value) -> {

            if (job.getExternalScheduler() == null) {
                job.setExternalScheduler(new HashMap<>());
            }

            job.getExternalScheduler().computeIfAbsent("profiler", (k) -> List.of(new HashMap<>())).get(0)
               .put(coLocated, value.toString());
        });

        return job;
    }


    public Stream<String> findCoLocationWithTheLeastRuntime(String jobName) {
        var coLocations = matrix.getOrDefault(jobName, Collections.emptyMap());
        return coLocations.entrySet().stream()
                          .sorted(Comparator.comparingLong(entry -> {
                              return entry.getValue().getAverageRuntimeInSeconds();
                          }))
                          .map(Map.Entry::getKey);


    }

    public void print() {
        var table = GridTable.of(matrix.size() + 1, matrix.size() + 1);
        int y = 1;
        for (String job1 : matrix.keySet()) {
            table.put(0, y, Cell.of(job1));
            table.applyToRow(y, VERTICAL_CENTER);
            int x = 1;
            for (var job2 : matrix.keySet()) {
                table.put(x, 0, Cell.of(job2));

                if (Objects.equals(job1, job2)) {
                    table.put(x, y, Cell.of("/"));
                }
                else {
                    var runtime = matrix.getOrDefault(job2, Collections.emptyMap())
                                        .computeIfAbsent(job1, k -> new JobRuntime());
                    table.put(x, y, Cell.of(
                            runtime.toString()
                    ));
                }

                table.apply(x, y, HORIZONTAL_CENTER);
                x++;
            }
            y++;
        }

        table = Border.DOUBLE_LINE.apply(table);
        Util.print(table);
    }
}
