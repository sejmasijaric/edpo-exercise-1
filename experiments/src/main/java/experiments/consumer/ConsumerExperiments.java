package experiments.consumer;

import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;

public class ConsumerExperiments {

    // Artificial delay per message (ms)
    private static final long PROCESSING_DELAY_MS =
            Long.parseLong(System.getenv().getOrDefault("PROCESSING_DELAY_MS", "0"));

    // Experiment duration in milliseconds
    private static final long EXPERIMENT_DURATION_MS =
            Long.parseLong(System.getenv().getOrDefault("EXPERIMENT_DURATION_MS", "30000"));

    public static void main(String[] args) throws IOException {

        String groupId = args.length > 0 ? args[0] : "group-default";

        KafkaConsumer<String, String> consumer;
        try (InputStream props = Resources.getResource("consumer.properties").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            consumer = new KafkaConsumer<>(properties);
        }

        consumer.subscribe(
                Arrays.asList("user-events", "global-events", "ack-event"),
                new LoggingRebalanceListener()
        );

        System.out.println("Consumer started in group: " + groupId);
        System.out.println("Processing delay: " + PROCESSING_DELAY_MS + " ms");
        System.out.println("Experiment duration: " + EXPERIMENT_DURATION_MS + " ms");

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < EXPERIMENT_DURATION_MS) {

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            if (records.isEmpty()) continue;

            // Track lag per partition
            Map<Integer, Long> partitionLagSum = new HashMap<>();
            Map<Integer, Integer> partitionCount = new HashMap<>();

            for (ConsumerRecord<String, String> record : records) {

                // Artificial delay per message
                if (PROCESSING_DELAY_MS > 0) {
                    try {
                        Thread.sleep(PROCESSING_DELAY_MS);
                    } catch (InterruptedException ignored) {}
                }

                long lag = calculateLag(consumer, record);

                // Accumulate lag per partition
                partitionLagSum.merge(record.partition(), lag, Long::sum);
                partitionCount.merge(record.partition(), 1, Integer::sum);
            }

            // Print average lag per partition for this batch
            for (Integer partition : partitionLagSum.keySet()) {
                long avgLag = partitionLagSum.get(partition) / partitionCount.get(partition);
                System.out.printf("Partition %d | avg lag: %d | messages processed: %d%n",
                        partition, avgLag, partitionCount.get(partition));
            }
        }

        consumer.close();
        System.out.println("Experiment finished.");
    }

    private static long calculateLag(KafkaConsumer<String, String> consumer,
                                     ConsumerRecord<String, String> record) {

        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singleton(tp));
        long latestOffset = endOffsets.get(tp);

        return latestOffset - record.offset() - 1;
    }

    // Rebalance listener
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