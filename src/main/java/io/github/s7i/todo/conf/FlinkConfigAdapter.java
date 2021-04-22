package io.github.s7i.todo.conf;

import io.github.s7i.todo.conf.KafkaTopic.Type;
import java.util.List;
import java.util.Properties;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;

public interface FlinkConfigAdapter {

    String AT_LEAST_ONCE = "AT_LEAST_ONCE";
    String ACTION = "action";
    String REACTION = "reaction";
    String TX_LOG = "txlog";

    List<KafkaTopic> getKafkaTopicList();

    default KafkaTopic lookup(String name, Type type) {
        return getKafkaTopicList().stream()
              .filter(type::is)
              .filter(s -> s.getName().equals(name))
              .findFirst()
              .orElseThrow();
    }

    default FlinkKafkaConsumer<String> actionSource() {
        var src = lookup(ACTION, Type.SOURCE);
        var pros = new Properties();
        pros.putAll(src.getProperties());
        return new FlinkKafkaConsumer<>(src.getTopic(), new SimpleStringSchema(), pros);
    }

    default FlinkKafkaProducer<String> sink() {
        var sink = lookup(REACTION, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return new FlinkKafkaProducer<>(
              topic,
              new StringSerializer(topic),
              props, Semantic.valueOf(semantic));
    }

    default FlinkKafkaProducer<String> txLog() {
        var sink = lookup(TX_LOG, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return new FlinkKafkaProducer<>(
              topic,
              new TxLogSerializer(topic),
              props, Semantic.valueOf(semantic)
        );
    }

}
