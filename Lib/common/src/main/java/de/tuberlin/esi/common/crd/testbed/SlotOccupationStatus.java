package de.tuberlin.esi.common.crd.testbed;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Data
@NoArgsConstructor
public class SlotOccupationStatus {

    @Nullable
    private String reservedFor;

    private SlotState state;
    private String podName;
    private String nodeName;
    private int nodeId;
    private int slotPositionOnNode;
    @Nullable
    private String podUId;
    private int position;

    public SlotOccupationStatus(SlotState state, String podName, String nodeName, int nodeId, int slotPositionOnNode,
                                @Nullable String podUId) {
        this.state = state;
        this.podName = podName;
        this.nodeName = nodeName;
        this.nodeId = nodeId;
        this.slotPositionOnNode = slotPositionOnNode;
        this.podUId = podUId;
    }

    public int getSlotPositionOnNode() {
        return slotPositionOnNode;
    }
}
