package io.github.s7i.todo.conf;

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

@RequiredArgsConstructor
public class StringSerializer implements KafkaSerializationSchema<String> {

    final String topic;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(String element, @Nullable Long timestamp) {
        return new ProducerRecord<>(topic, 0, null, element.getBytes(StandardCharsets.UTF_8));
    }
}
