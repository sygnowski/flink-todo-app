package io.github.s7i.todo.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Configuration implements FlinkConfigAdapter {

    @SneakyThrows
    public static Configuration from(String content) {
        log.info("Configuration: \n{}", content);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(content, Configuration.class);
    }

    @SneakyThrows
    public static Configuration fromResources() {
        String content;
        try (var io = Configuration.class.getResourceAsStream("/config.yml")) {
            var baos = new ByteArrayOutputStream();
            byte[] buff = new byte[1024 * 3];
            for (int n; (n = io.read(buff)) > 0; ) {
                baos.write(buff, 0, n);
            }
            content = baos.toString(StandardCharsets.UTF_8);
        }
        return from(content);
    }

    @JsonProperty("kafka-io")
    private List<KafkaTopic> kafkaTopicList;

}
