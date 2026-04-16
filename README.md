# EDPO Exercise 1 - Kafka Experiments

This repository contains a small Java/Maven project for exploring Apache Kafka producer, consumer, and fault-tolerance behavior.

The code is organized around runnable experiment classes that send and consume messages from local Kafka clusters started with Docker Compose. The experiments cover basic message flow, producer acknowledgements, producer batching, consumer group lag/rebalancing, and broker failure behavior.

## Project Structure

```text
.
+-- README.md
+-- experiments
    +-- docker
    |   +-- docker-compose.yml
    |   +-- docker-compose-multi-broker.yml
    +-- pom.xml
    +-- src/main
        +-- java
        |   +-- core
        |   |   +-- Consumer.java
        |   |   +-- Producer.java
        |   +-- experiments
        |       +-- consumer
        |       |   +-- ConsumerExperiments.java
        |       +-- fault_tolerance
        |       |   +-- FailureExperiment.java
        |       +-- producer
        |           +-- AcksExperiment.java
        |           +-- BatchSizeExperiment.java
        +-- resources
            +-- consumer.properties
            +-- producer.properties
```

## Prerequisites

- Java 17
- Maven
- Docker and Docker Compose

Check your local versions:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## Start Kafka

For most experiments, start the single-broker Kafka cluster:

```bash
cd experiments
docker compose -f docker/docker-compose.yml up -d
```

Kafka will be available at:

```text
localhost:9092
```

Stop it when finished:

```bash
docker compose -f docker/docker-compose.yml down
```

## Build

From the `experiments` directory:

```bash
mvn clean package
```

The project does not currently define automated tests, so the build mainly compiles the Java sources and packages the application.

## Run the Basic Producer and Consumer

Open two terminals.

Terminal 1 - start the consumer:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=core.Consumer
```

Terminal 2 - send messages:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=core.Producer
```

The producer writes to:

- `user-events`
- `global-events`

The consumer subscribes to:

- `user-events`
- `global-events`
- `ack-event`

## Producer Experiments

### Acknowledgement Experiment

Runs the same producer workload with different Kafka acknowledgement settings:

- `acks=0`
- `acks=1`
- `acks=all`

Run:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=experiments.producer.AcksExperiment
```

The output reports total time and throughput in messages per second for each acknowledgement mode.

### Batch Size Experiment

Compares producer throughput across combinations of:

- `batch.size`: `4096`, `16384`, `65536`
- `linger.ms`: `0`, `10`, `50`
- artificial production delay: `0`, `2`, `5` ms

Run:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=experiments.producer.BatchSizeExperiment
```

The experiment sends messages to the `user-events` topic and prints timing and throughput for each configuration.

## Consumer Experiments

`ConsumerExperiments` measures consumer lag per partition and logs partition assignments/revocations during consumer group rebalances.

Run one consumer:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=experiments.consumer.ConsumerExperiments -Dexec.args="group-1"
```

Run another consumer in a second terminal with the same group id to observe rebalancing:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=experiments.consumer.ConsumerExperiments -Dexec.args="group-1"
```

Optional environment variables:

```bash
PROCESSING_DELAY_MS=10 EXPERIMENT_DURATION_MS=60000 \
mvn exec:java -Dexec.mainClass=experiments.consumer.ConsumerExperiments -Dexec.args="group-1"
```

- `PROCESSING_DELAY_MS` adds an artificial per-message processing delay.
- `EXPERIMENT_DURATION_MS` controls how long the experiment runs.

Generate messages while the consumers are running:

```bash
mvn exec:java -Dexec.mainClass=core.Producer
```

## Fault-Tolerance Experiment

For broker-failure experiments, use the multi-broker Kafka setup:

```bash
cd experiments
docker compose -f docker/docker-compose-multi-broker.yml up -d
```

This starts:

- one controller container: `controller`
- two broker containers: `kafka1`, `kafka2`
- external broker ports: `localhost:9092`, `localhost:9094`

Start the fault-tolerance consumer:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=experiments.fault_tolerance.FailureExperiment -Dexec.args="fault-group"
```

In another terminal, produce messages:

```bash
cd experiments
mvn exec:java -Dexec.mainClass=core.Producer
```

Then stop a broker to observe failure detection, lag reporting, and rebalancing behavior:

```bash
docker stop kafka1
```

Bring it back:

```bash
docker start kafka1
```

Stop the multi-broker cluster when finished:

```bash
docker compose -f docker/docker-compose-multi-broker.yml down
```

## Configuration

Kafka client defaults live in:

- `experiments/src/main/resources/producer.properties`
- `experiments/src/main/resources/consumer.properties`

Important defaults:

- Kafka bootstrap server: `localhost:9092`
- consumer offset reset: `earliest`
- consumer auto commit: enabled
- consumer group id in `consumer.properties`: `test`

Some experiment classes override selected settings in code, such as `acks`, `batch.size`, `linger.ms`, group id, and timeout values.

## Troubleshooting

If Maven cannot connect to Kafka, make sure the Docker cluster is running:

```bash
docker compose -f docker/docker-compose.yml ps
```

If topics have stale data from earlier runs, remove the Kafka containers and start fresh:

```bash
docker compose -f docker/docker-compose.yml down
docker compose -f docker/docker-compose.yml up -d
```

If `mvn exec:java` is not available locally yet, Maven may need to download the exec plugin the first time it runs.
