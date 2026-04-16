package experiments.producer;

import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

public class BatchSizeExperiment {

    private static final int NUM_MESSAGES = 5_000;
    private static final String TOPIC = "user-events";

    public static void main(String[] args) throws Exception {
        int[] batchSizes = {4096, 16384, 65536};
        int[] lingerValues = {0, 10, 50};
        int[] productionDelaysMs = {0, 2, 5};

        for (int batchSize : batchSizes) {
            for (int lingerMs : lingerValues) {
                for (int productionDelayMs : productionDelaysMs) {
                    runExperiment(batchSize, lingerMs, productionDelayMs);
                    Thread.sleep(5000);
                }
            }
        }
    }

    private static void runExperiment(int batchSize, int lingerMs, int productionDelayMs) throws Exception {
        Properties props;
        try (InputStream input = Resources.getResource("producer.properties").openStream()) {
            props = new Properties();
            props.load(input);
        }

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        System.out.println("\n=== Batch Size Experiment ===");
        System.out.println("batch.size = " + batchSize);
        System.out.println("linger.ms = " + lingerMs);
        System.out.println("production delay = " + productionDelayMs + " ms");

        long startTime = System.currentTimeMillis();
        List<Future<RecordMetadata>> futures = new ArrayList<>(NUM_MESSAGES);

        for (int i = 0; i < NUM_MESSAGES; i++) {
            futures.add(
                    producer.send(new ProducerRecord<>(
                            TOPIC,
                            "user_id_" + i,
                            "value_" + System.nanoTime()
                    ))
            );

            if (productionDelayMs > 0) {
                Thread.sleep(productionDelayMs);
            }
        }

        producer.flush();

        // Wait for all sends to complete
        for (Future<RecordMetadata> future : futures) {
            future.get();
        }

        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        double throughput = NUM_MESSAGES * 1000.0 / durationMs;

        System.out.println("Total time (ms): " + durationMs);
        System.out.println("Throughput (msg/sec): " + throughput);

        producer.close();
    }
}