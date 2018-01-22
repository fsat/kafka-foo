# Playground: Akka Streams + Kafka

This sample project is a playground to experiment with connecting [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html) to [Kafka](http://kafka.apache.org/) using [Reactive Kafka](https://github.com/akka/reactive-kafka).

## Pre-requisite

A running Kafka on `localhost:9092` with a topic called `test` created.

Follow the [Kafka Quickstart](http://kafka.apache.org/quickstart) to get Kafka to run on your machine and topic created.

A single broker is sufficient for this project, i.e. no need to start multiple broker cluster.

## How to run

Ensure the following is running on a separate terminal:

```bash
$ bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test
```

This will display the message you are entering via `STDIN` after it's been routed through Kafka.

### Simple STDIN stream to Kafka

```bash
$ sbt "run-main test.Simple"
```

### STDIN to Kafka via Actor

```bash
$ sbt "run-main test.ViaActor"
```

The `test.ViaActor` has `test.actor.KafkaPublish` actor encapsulates a single stream to Kafka. This provides the following additional functionality:

* Allows reconnecting to Kafka should there be failure.
* Caches the message to be published until connectivity to Kafka is established. 

