package io.github.s7i.todo.conf;

import java.util.List;
import java.util.Properties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

public interface FlinkConfigAdapter {

    List<KafkaTopic> getKafkaTopicList();

    default FlinkKafkaConsumer<String> actionSource() {
        var src = getKafkaTopicList().stream()
              .filter(KafkaTopic::isSource)
              .filter(s -> s.getName().equals("action"))
              .findFirst()
              .orElseThrow();
        var pros = new Properties();
        pros.putAll(src.getProperties());
        return new FlinkKafkaConsumer<>(src.getTopic(), new SimpleStringSchema(), pros);
    }

    default FlinkKafkaProducer<String> sink() {
        var sink = getKafkaTopicList().stream()
              .filter(KafkaTopic::isSink)
              .filter(s -> s.getName().equals("reaction"))
              .findFirst()
              .orElseThrow();
        var topic = sink.getTopic();
        var props = new Properties();
        props.putAll(sink.getProperties());
        return new FlinkKafkaProducer<>(topic, new SimpleStringSchema(), props);
    }

}
