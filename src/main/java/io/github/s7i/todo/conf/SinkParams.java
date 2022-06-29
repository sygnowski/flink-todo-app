package io.github.s7i.todo.conf;

import java.util.Optional;
import java.util.Properties;
import lombok.Builder;
import lombok.Getter;
import org.apache.flink.api.common.serialization.SerializationSchema;

@Builder
@Getter
public class SinkParams {

    private final Properties props;
    private final String topic;
    private final String semantic;
    private final SerializationSchema<String> keySerialization;

    public Optional<SerializationSchema<String>> getKeySerialization() {
        return Optional.ofNullable(keySerialization);
    }
}
