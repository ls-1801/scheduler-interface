package de.tuberlin.esi.common.statemachine;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@RequiredArgsConstructor
public class ErrorsOrResult {
    List<String> errors;
    Boolean result;

    public static ErrorsOrResult error(List<String> errors) {
        return new ErrorsOrResult(errors, null);
    }

    public static ErrorsOrResult result(Boolean result) {
        return new ErrorsOrResult(null, result);
    }

    public boolean isError() {
        return errors != null;
    }

    public boolean isResult() {
        return result != null;
    }
}
