package io.github.s7i.todo.conf;

import java.util.List;
import java.util.Properties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;

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

    default FlinkKafkaProducer<String> sink(String semantic) {
        var sink = getKafkaTopicList().stream()
              .filter(KafkaTopic::isSink)
              .filter(s -> s.getName().equals("reaction"))
              .findFirst()
              .orElseThrow();
        var topic = sink.getTopic();
        var props = new Properties();
        props.putAll(sink.getProperties());
        return new FlinkKafkaProducer<>(
              topic,
              new StringSerializer(topic),
              props, Semantic.valueOf(semantic));
    }

    default FlinkKafkaProducer<String> txLog(String semantic) {
        var sink = getKafkaTopicList().stream()
              .filter(KafkaTopic::isSink)
              .filter(s -> s.getName().equals("txlog"))
              .findFirst()
              .orElseThrow();
        var topic = sink.getTopic();
        var props = new Properties();
        props.putAll(sink.getProperties());
        return new FlinkKafkaProducer<>(
              topic,
              new TxLogSerializer(topic),
              props, Semantic.valueOf(semantic)
        );
    }

}
