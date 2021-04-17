package io.github.s7i.todo.conf;

import java.util.Map;
import lombok.Data;

@Data
public class KafkaTopic {

    private String name;
    private String type;
    private String topic;
    private Map<String, String> properties;

    public boolean isSource() {
        return "source".equals(type);
    }

    public boolean isSink() {
        return "sink".equals(type);
    }
}
