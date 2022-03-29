package de.tuberlin.batchjoboperator.examplescheduler;

import com.google.common.primitives.Longs;
import de.tuberlin.batchjoboperator.common.crd.batchjob.ScheduledEvents;
import lombok.Getter;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

@Getter
public class JobRuntime {
    @Nullable
    private Long averageRuntimeInSeconds;

    private Long nSamples = 1L;

    public JobRuntime() {
        averageRuntimeInSeconds = 0L;
        nSamples = 0L;
    }

    public JobRuntime(String value) {
        var pattern = Pattern.compile("(\\d+)( \\((\\d+)\\))?");
        var matcher = pattern.matcher(value);
        if (!matcher.find())
            throw new RuntimeException("Job Run time does not match pattern");

        this.averageRuntimeInSeconds = Longs.tryParse(matcher.group(1));
        if (matcher.group(3) == null || matcher.group(3).isEmpty()) {
            this.nSamples = 1L;
        }
        else {
            this.nSamples = Longs.tryParse(matcher.group(3));
        }

    }

    public void update(ScheduledEvents lastEvent) {
        var duration = ChronoUnit.SECONDS.between(Instant.parse(lastEvent.getStart()),
                Instant.parse(lastEvent.getStop()));

        this.averageRuntimeInSeconds = ((this.averageRuntimeInSeconds * nSamples) + duration) / (nSamples + 1);
        this.nSamples++;
    }

    @Override
    public String toString() {
        return averageRuntimeInSeconds + " (" + nSamples + ")";
    }
}
