package io.github.s7i.todo.conf;

import io.github.s7i.todo.conf.KafkaTopic.Type;
import java.util.List;
import java.util.Properties;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;

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

    default SourceFunction<String> actionSource() {
        var src = lookup(ACTION, Type.SOURCE);
        var pros = new Properties();
        pros.putAll(src.getProperties());
        return kafkaSource(src.getTopic(), new SimpleStringSchema(), pros);
    }

    default SinkFunction<String> sink() {
        var sink = lookup(REACTION, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return kafkaSink(
              topic,
              new StringSerializer(topic),
              props, Semantic.valueOf(semantic));
    }

    default SinkFunction<String> txLog() {
        var sink = lookup(TX_LOG, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return kafkaSink(
              topic,
              new TxLogSerializer(topic),
              props, Semantic.valueOf(semantic)
        );
    }

    default <OUT> SourceFunction<OUT> kafkaSource(String topic, DeserializationSchema<OUT> deserializer, Properties props) {
        return new FlinkKafkaConsumer<>(topic, deserializer, props);
    }

    default <OUT> SourceFunction<OUT> kafkaSource(String topic, KafkaDeserializationSchema<OUT> deserializer, Properties props) {
        return new FlinkKafkaConsumer<>(topic, deserializer, props);
    }

    default <IN> SinkFunction<IN> kafkaSink(String topic, KafkaSerializationSchema<IN> serializer, Properties props, Semantic semantic) {
        return new FlinkKafkaProducer<IN>(
              topic,
              serializer,
              props,
              semantic);
    }
}
