package io.github.s7i.todo.conf;

import io.github.s7i.todo.domain.TxLog;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.kafka.clients.producer.ProducerRecord;

@RequiredArgsConstructor
@Slf4j
public class TxLogSerializer implements KafkaSerializationSchema<String> {

    final String topic;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(String element, @Nullable Long timestamp) {
        log.info("serializing: {}", element);
        try {
            String key = TxLog.from(element).getTodo().getId();
            return new ProducerRecord<>(
                  topic,
                  0,
                  key.getBytes(StandardCharsets.UTF_8),
                  element.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new FlinkRuntimeException(e);
        }
    }
}
