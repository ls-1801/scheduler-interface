package de.tuberlin.esi.common.crd.scheduling;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobConditionValue {
    private NamespacedName name;
    private Boolean value;
}
