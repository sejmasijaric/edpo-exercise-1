package experiments.producer;

import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.*;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Future;

public class AcksExperiment {

    private static final int NUM_MESSAGES = 1000;
    private static final String TOPIC = "ack-event";

    public static void main(String[] args) throws Exception {

        String[] acksConfigs = {"0", "1", "all"};

        for (String acks : acksConfigs) {
            runExperiment(acks);
            Thread.sleep(5000);
        }
    }

    private static void runExperiment(String acks) throws Exception {

        Properties props;
        try (InputStream input = Resources.getResource("producer.properties").openStream()) {
            props = new Properties();
            props.load(input);
        }

        props.put(ProducerConfig.ACKS_CONFIG, acks);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        System.out.println("\n=== Acks Experiment ===");
        System.out.println("acks = " + acks);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_MESSAGES; i++) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(TOPIC,
                            "user_id_" + i,
                            "value_" + System.nanoTime());

            producer.send(record).get(); // wait for ack
        }

        producer.flush();
        long endTime = System.currentTimeMillis();

        long durationMs = endTime - startTime;
        double throughput = NUM_MESSAGES * 1000.0 / durationMs;

        System.out.println("Total time (ms): " + durationMs);
        System.out.println("Throughput (msg/sec): " + throughput);

        producer.close();
    }
}