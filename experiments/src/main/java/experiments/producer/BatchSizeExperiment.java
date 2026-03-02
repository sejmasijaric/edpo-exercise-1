package experiments.producer;

import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Future;

public class BatchSizeExperiment {

    private static final int NUM_MESSAGES = 100_000;
    private static final String TOPIC = "user-events";

    public static void main(String[] args) throws Exception {

        int[] batchSizes = {4096, 16384, 65536}; // small → large

        for (int batchSize : batchSizes) {
            runExperiment(batchSize);
            Thread.sleep(5000);
        }
    }

    private static void runExperiment(int batchSize) throws Exception {

        Properties props;
        try (InputStream input = Resources.getResource("producer.properties").openStream()) {
            props = new Properties();
            props.load(input);
        }

        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        System.out.println("\n=== Batch Size Experiment ===");
        System.out.println("batch.size = " + batchSize);

        long startTime = System.currentTimeMillis();
        long totalLatencyNs = 0;

        for (int i = 0; i < NUM_MESSAGES; i++) {
            long sendStart = System.nanoTime();

            Future<RecordMetadata> future = producer.send(
                    new ProducerRecord<>(TOPIC,
                            "user_id_" + i,
                            "value_" + System.nanoTime())
            );

            future.get(); // ensures latency measurement

            long sendEnd = System.nanoTime();
            totalLatencyNs += (sendEnd - sendStart);
        }

        producer.flush();
        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        double throughput = NUM_MESSAGES * 1000.0 / durationMs;
        double avgLatencyMs = (totalLatencyNs / 1_000_000.0) / NUM_MESSAGES;

        System.out.println("Total time (ms): " + durationMs);
        System.out.println("Throughput (msg/sec): " + throughput);
        System.out.println("Avg latency (ms): " + avgLatencyMs);

        producer.close();
    }
}