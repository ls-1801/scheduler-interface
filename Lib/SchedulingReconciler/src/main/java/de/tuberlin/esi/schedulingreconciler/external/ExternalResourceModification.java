package de.tuberlin.esi.schedulingreconciler.external;

import io.fabric8.kubernetes.client.Watcher;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@RequiredArgsConstructor
@Value
@Builder
@Jacksonized
public class ExternalResourceModification<T> {
    Watcher.Action action;
    T resource;
}
