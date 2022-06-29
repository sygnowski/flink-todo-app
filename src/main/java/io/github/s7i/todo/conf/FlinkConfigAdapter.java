package io.github.s7i.todo.conf;

import io.github.s7i.todo.conf.KafkaTopic.Type;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;

import java.util.List;
import java.util.Properties;

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

    default KafkaSource<String> actionSource() {
        var src = lookup(ACTION, Type.SOURCE);
        var pros = new Properties();
        pros.putAll(src.getProperties());

        return KafkaSource.<String>builder()
              .setProperties(pros)
              .setTopics(src.getTopic())
              .setValueOnlyDeserializer(new SimpleStringSchema())
              .build();
    }

    default KafkaSink<String> sink() {
        var sink = lookup(REACTION, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return buildSink(props, topic, semantic);
    }

    default KafkaSink<String> txLog() {
        var sink = lookup(TX_LOG, Type.SINK);
        var topic = sink.getTopic();
        var semantic = sink.getSemantic(AT_LEAST_ONCE);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return buildSink(props, topic, semantic);
    }

    private KafkaSink<String> buildSink(Properties props, String topic, String semantic) {
        return KafkaSink.<String>builder()
              .setBootstrapServers(props.getProperty("bootstrap.servers"))
              .setKafkaProducerConfig(props)
              .setRecordSerializer(KafkaRecordSerializationSchema.<String>builder()
                    .setTopic(topic)
                    .setValueSerializationSchema(new SimpleStringSchema())
                    .setKeySerializationSchema(new SimpleStringSchema())
                    .build())
              .setDeliverGuarantee(DeliveryGuarantee.valueOf(semantic))
              .build();
    }

}
