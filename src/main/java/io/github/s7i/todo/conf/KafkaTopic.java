package io.github.s7i.todo.conf;

import static java.util.Objects.isNull;

import java.util.Map;
import lombok.Data;

@Data
public class KafkaTopic {

    public enum Type {
        SOURCE, SINK;

        public boolean is(KafkaTopic kafkaTopic) {
            return this == SOURCE ?
                  kafkaTopic.isSource()
                  : kafkaTopic.isSink();
        }
    }

    private String name;
    private String type;
    private String topic;
    private Map<String, String> properties;
    private String semantic;

    public String getSemantic(String ifNotPresent) {
        return isNull(semantic) ? ifNotPresent : semantic;
    }

    public boolean isSource() {
        return Type.SOURCE.name().toLowerCase().equals(type);
    }

    public boolean isSink() {
        return Type.SINK.name().toLowerCase().equals(type);
    }
}
