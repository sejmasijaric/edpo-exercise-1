package experiments.fault_tolerance;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.*;

public class FailureExperiment {

    private static final long PROCESSING_DELAY_MS =
            Long.parseLong(System.getenv().getOrDefault("PROCESSING_DELAY_MS", "0"));
    private static final long EXPERIMENT_DURATION_MS =
            Long.parseLong(System.getenv().getOrDefault("EXPERIMENT_DURATION_MS", "60000"));

    public static void main(String[] args) {

        String groupId = args.length > 0 ? args[0] : "fault-group";

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        // Shorten timeouts so failures are detected faster
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000"); // 10s
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "12000"); // 12s
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000"); // 30s

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("user-events", "global-events"), new LoggingRebalanceListener());

        System.out.println("Fault tolerance consumer started in group: " + groupId);
        System.out.println("Processing delay: " + PROCESSING_DELAY_MS + " ms");
        System.out.println("Experiment duration: " + EXPERIMENT_DURATION_MS + " ms");

        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < EXPERIMENT_DURATION_MS) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    if (records.isEmpty()) continue;

                    for (ConsumerRecord<String, String> record : records) {

                        // Simulate processing delay
                        if (PROCESSING_DELAY_MS > 0) {
                            Thread.sleep(PROCESSING_DELAY_MS);
                        }

                        long lag = calculateLag(consumer, record);
                        if (lag < 0) {
                            System.out.printf("⚠️ Partition %s-%d temporarily unavailable%n",
                                    record.topic(), record.partition());
                        } else {
                            System.out.printf("Topic: %s | Partition: %d | Offset: %d | Lag: %d%n",
                                    record.topic(), record.partition(), record.offset(), lag);
                        }
                    }

                } catch (WakeupException e) {
                    // Ignore, used to shut down consumer
                } catch (Exception e) {
                    System.out.println("⚠️ Exception detected during poll: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            consumer.close();
            System.out.println("Experiment finished.");
        }
    }

    private static long calculateLag(KafkaConsumer<String, String> consumer, ConsumerRecord<String, String> record) {
        try {
            TopicPartition tp = new TopicPartition(record.topic(), record.partition());
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singleton(tp));
            long latestOffset = endOffsets.get(tp);
            return latestOffset - record.offset() - 1;
        } catch (Exception e) {
            // Could be leader unavailable
            return -1;
        }
    }

    static class LoggingRebalanceListener implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            System.out.println("⚠️ Partitions revoked: " + partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            System.out.println("✅ Partitions assigned: " + partitions);
        }
    }
}