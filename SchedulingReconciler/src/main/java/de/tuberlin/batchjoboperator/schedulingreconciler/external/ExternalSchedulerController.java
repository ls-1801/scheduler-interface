//
//package de.tuberlin.batchjoboperator.schedulingreconciler.external;
//
//import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
//import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
//import io.fabric8.kubernetes.api.model.HasMetadata;
//import io.fabric8.kubernetes.api.model.KubernetesResourceList;
//import io.fabric8.kubernetes.api.model.Node;
//import io.fabric8.kubernetes.api.model.Quantity;
//import io.fabric8.kubernetes.client.KubernetesClient;
//import lombok.RequiredArgsConstructor;
//import lombok.Value;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/scheduler")
//@RequiredArgsConstructor
//@Slf4j
//public class ExternalSchedulerController {
//
//    private final KubernetesClient client;
//    private final boolean failOnProblem = true;
//
//    private final Lock schedulingInProgressLock = new ReentrantLock();
//
//
//
////    @GetMapping(value = "/queue")
////    public List<NamespacedName> getQueue() {
////        var list = client.resources(BatchJob.class)
////                         //Sadly this does not seem to be supported at the moment
////                         //.withField("Status.State", BatchJobState.InQueueState.name())
////                         .list();
////
////        return list.getItems().stream()
////                   .filter(job -> BatchJobState.InQueueState.equals(job.getStatus().getState()))
////                   .map(NamespacedName::of)
////                   .collect(Collectors.toList());
////
////    }
//
//    private <T extends HasMetadata> T verifySingleResource(KubernetesResourceList<T> list) {
//        if (list.getItems().size() != 1) {
//            throw new RuntimeException("Multiple Resources exist but a single is expected");
//        }
//
//        return list.getItems().get(0);
//    }
//
////    @GetMapping(value = "/slots")
////    public List<ExternalSlotPerNodeView> getSlots() {
////        var list = client.resources(Slot.class)
////                         .list();
////        var slot = verifySingleResource(list);
////
////        return slot.getStatus().getSlots().stream()
////                   .collect(Collectors.groupingBy(SlotOccupationStatus::getNodeName, Collectors.counting()))
////                   .entrySet()
////                   .stream()
////                   .map(e -> new ExternalSlotPerNodeView(e.getKey(), e.getValue()))
////                   .collect(Collectors.toList());
////    }
//
//    @GetMapping(value = "/nodes")
//    public List<ExternalNodeView> getNodes() {
//        var nodes = client.nodes().list();
//
//        return nodes.getItems().stream()
//                    .map(ExternalNodeView::new)
//                    .collect(Collectors.toList());
//    }
//
////    private void executeScheduling(VerifiedReleaseFromQueueRequest request) {
////        var reporter = new SlotStatusReporter(client, request.getSlot());
////
////        reporter.schedulingInProgress(request.getRequest().getJobs());
////        try {
////            request.getJobSlotPairing()
////                   .forEach((job, freeSlots) ->
////                           ApplicationBuilder.forJob(job, request.getSlot())
////                                             .inNamespace(getNamespace())
////                                             .inSlots(freeSlots.stream().map(SlotOccupationStatus::getPosition)
////                                                               .collect(Collectors.toSet()))
////                                             .create(client));
////        } catch (Exception e) {
////            log.error("Problem during Application CR creation", e);
////            reporter.abortWithProblem(e);
////            throw e;
////        }
////    }
//
////    @PostMapping(value = "/release-from-queue")
////    public void releaseFromQueue(@RequestBody ReleaseFromQueueRequest releaseFromQueueRequest) {
////        if (schedulingInProgressLock.tryLock()) {
////            try {
////                var request = verifyRequest(releaseFromQueueRequest, getNamespace(), client);
////                executeScheduling(request);
////            } finally {
////                schedulingInProgressLock.unlock();
////            }
////        }
////        else {
////            throw new RuntimeException("Scheduling in progress!");
////        }
////
////    }
//
//    @GetMapping(value = "/state")
//    public SlotStatus stateOfApplications() {
//        return verifySingleResource(client.resources(Slot.class).list()).getStatus();
//    }
//
//    private String getNamespace() {
//        return "default";
//    }
//
//    @Value
//    @RequiredArgsConstructor
//    public static class ExternalSlotPerNodeView {
//        String nodeHostName;
//        Long slotsPerNode;
//    }
//
//    @Value
//    public static class ExternalNodeView {
//        String nodeHostName;
//        BigDecimal availableVirtualCores;
//
//        public ExternalNodeView(Node node) {
//            this.nodeHostName = node.getMetadata().getName();
//            this.availableVirtualCores =
//                    Quantity.getAmountInBytes(node.getStatus().getAllocatable().get("cpu"));
//        }
//    }
//}
//
