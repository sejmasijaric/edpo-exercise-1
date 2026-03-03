# Event-Driven and Process Oriented Architecture - Exercise 1 
## Getting started with Kafka

For this exercise, we needed to perform a number of experiments on the producer and consumer. Besides that we needed to experiment with Kafka's fault tolerance. 

All the code is placed in the experiments/ folder. This folder contains the docker compose files to start Kafka in docker and the source code to run the experiments. There are two docker compose file:
- to start one broker (broker and controller combined)
- to start multiple brokers (broker and controller splitted)

The classes in experiments/src/main/java/core folder are used to produce and consume events with Kafka. In the experiments/src/main/java/experiments folder is the implementation for the experiments divided based on category: 
- producer
- consumer

To test the producer, the batch sizes and the acknowledgement confiugrations were manipulated. The consumer was tested by introducing artificial delays and lag. Adding and removing consumers on runtime was also observed in order to experiment with rebalencing of partitions. In order to test fault tolerance, while the consumer was consuming event, the broker was stopped. As a result the consumer silently waited for the broker to be available again. Once the broker was restarted, the consumer consumed the rest of the events almost immediately without dropping events.

This was a fun way to test out and play with Kafka.
