package de.tuberlin.batchjoboperator.common.constants;

import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.PREFIX;

public class SlotsConstants {

    //    private static final String GHOST_POD_LABEL_NAME = "batchjob.tuberlin.de/ghostpod";
//    private static final String GHOST_POD_LABEL_VALUE = "true";
//
//    private static final String GHOST_POD_GENERATION_ANNOTATION_NAME =
//            "batchjob.tuberlin.de/latest-observerd-resource-version";
//    private static final String GHOST_POD_LABEL_TARGET_NODE_NAME = "batchjob.tuberlin.de/target-node";
//    public static final String GHOST_POD_LABEL_POD_ID_NAME = "batchjob.tuberlin.de/pod-id";
    public static final String SLOT_POD_LABEL_NAME = PREFIX + "slots-resource-name";
    public static final String SLOT_IDS_NAME = PREFIX + "slots-ids";
    public static final String SLOT_POD_IS_GHOSTPOD_NAME = PREFIX + "slots-is-ghost-pod";
    public static final String SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME = PREFIX +
            "slots-ghost-pod-will-be-preempted-by";
    public static final String SLOT_POD_TARGET_NODE_NAME = PREFIX + "slots-target-node";
    public static final String SLOT_POD_SLOT_ID_NAME = PREFIX + "slots-id";
    public static final String SLOT_POD_GENERATION_NAME = PREFIX + "slots-latest-observed-resource-version";
    public static final String GHOST_POD_NAME_PREFIX = "ghostpod";
}
