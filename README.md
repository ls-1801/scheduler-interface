External-Scheduler-Interface

### Evaluation setup

Create the Testbed resources

```yaml
apiVersion: batchjob.gcr.io/v1alpha1
kind: Slot
metadata:
  name: scheduler-slots
  namespace: default
spec:
  slotsPerNode: 2
  nodeLabel: "tuberlin.de/scheduler-slots"
  resourcesPerSlot:
    cpu: "600m"
    memory: "1.75Gi"
```

```yaml
apiVersion: batchjob.gcr.io/v1alpha1
kind: Slot
metadata:
  name: profiler-slots
  namespace: default
spec:
  slotsPerNode: 2
  nodeLabel: "tuberlin.de/profiler-slots"
  resourcesPerSlot:
    cpu: "600m"
    memory: "1.75Gi"
```

Either update Nodes manually and add the corresponding labels, `tuberlin.de/node-with-slots`
and `tuberlin. de/profiler-slots` or use the debug controller (which requires RBAC permissions to be configured).

Slots are ordered based on the label value. For the evaluation the Scheduler Testbed had three nodes with the scheduler
label.

```yaml
apiVersion: v1
kind: Node
metadata:
  # ... annotations etc.
  labels:
    # ... other labels
    tuberlin.de/scheduler-slots: "0"
  name: gke-opcluster-pool-1-b70952b6-cst0
spec:
# ...
```

Alternatively, the `/debug/node-set-up endpoint can be used. The debug controller finds Nodes with the least resources
requested and fails if not enough nodes exist.

The debug controller is part of the Testbed Operator and probably needs to be made accessible from outside the cluster.
During development, `kubectl port-forward TESTBED-OPERATOR-POD 8080:8080` was used to access the debug controller from
outside the cluster.

> GET http://localhost:8080/debug/node-set-up?name=batchjob-slots&count=3

> GET http://localhost:8080/debug/node-set-up?name=profiler-slots&count=1

To build the Example Scheduler

> mvn install -pl ExampleScheduler -am

The external interface needs to accessible, since there is no proper authorization, it is again advised to either deploy
the Example Scheduler inside the kubernetes cluster or to `kubectl port-forward SCHEDULING-OPERATOR-POD 8082:8082`.
After building the Example Scheduler the executable jar file is located in the ExampleScheduler/target folder.

> java -jar ExampleScheduler-0.0.1-SNAPSHOT-jar-with-dependencies.jar http://localhost:8082

The example scheduler expects `profiler-slots` and `scheduler-slots` Testbeds to exist, however the -p and -s argument
specifies different Testbeds.