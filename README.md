External-Scheduler-Interface

### Structure

The Lib module contains the implementation of the 3 reconcilers. Code which is shared between reconcilers is placed
inside the common module. The integration test modules contains only integration tests, and the Mock Scheduler
Implementation.

The Web module is used to build the Spring Boot Applications, the modules were seperated from one another because the
jar produced by the Spring Boot plugin could not be used for the integration test module and the external scheduler. The
Web modules contain web specific configurations. Like the actuator endpoints, and the server port.

The ExampleScheduler Module contains the example scheduler used in the evaluation. It depends on the
SchedulingReconciler which contains the External-Scheduler-Interface classes, which are used for serialization and
deserialization.

The Helm Module contains the Helm Chart and example BatchJobs, Schedulings, and Testbeds.

### Installation

Installation using the helm chart was tested on a fresh cluster

The Helm chart located in the Helm module. The Helm chart will additionally install the Spark and Flink Operator.

Images are publicly available in the gcr.io/spark-on-kubernetes-316714 repository. No build is required.

To install the helm chart in the test-namespace run: (this will create the namespace, remove --create-namespace if
namespace already exists)

> helm install -n test-namespace --create-namespace  esi .

#### Notes:

The namespace also needs to be adjusted inside the value.yaml. Currently, most of the Interface uses the Helm Releases
Namespace. However, the Flink and Spark Operator need to be instructed which namespace to watch for Applications.

The Spark-Operator Service account is dependent on the release name. The Service account is specified inside the
BatchJob Manifest and may need to be adjusted if the `SparkApplication` fails due to the service account not existing.
If the installation uses the `esi` release name the spark operator service account is called `esi-spark-operator`

### Evaluation setup

Create the Testbed resources

```yaml
apiVersion: esi.tu-berlin.de/v1alpha1
kind: Testbed
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
apiVersion: esi.tu-berlin.de/v1alpha1
kind: Testbed
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

### Development

To build the Operators and push the images the image registry run the following command. Unfortunately, tests are
current state are not passing reliable and are skipped. The probably needs to be configured, unless authorized to push
images to the registry `-Dbuild.image.registry=yourregistry`.

> mvn clean install -DskipTests jib:build

CRDs are only generated if the generate-crds profile is specified. CRDs are built from the commons module and copied
into the Helm module.

> mvn clean install -pl Helm -am -Pgenerate-crds
