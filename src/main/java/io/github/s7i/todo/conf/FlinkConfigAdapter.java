package io.github.s7i.todo.conf;

import io.github.s7i.todo.conf.KafkaTopic.Type;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.List;
import java.util.Properties;

public interface FlinkConfigAdapter {

    String AT_LEAST_ONCE = "AT_LEAST_ONCE";
    String ACTION = "action";
    String REACTION = "reaction";
    String TX_LOG = "txlog";
    String BOOTSTRAP_SERVERS = "bootstrap.servers";

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

    default SingleOutputStreamOperator<String> buildSourceStream(StreamExecutionEnvironment env, WatermarkStrategy<String> wms, String sourceName) {
        return env.fromSource(actionSource(), wms, "Action Source");
    }

    default Sink<String> sinkOfReaction() {
        return sink(REACTION, null);
    }

    default Sink<String> txLog() {
        return sink(TX_LOG, new TxLogKeySerializer());
    }

    default Sink<String> sink(String name, SerializationSchema<String> keySchema) {
        var sink = lookup(name, Type.SINK);
        var props = new Properties();
        props.putAll(sink.getProperties());
        return buildSink(SinkParams.builder()
              .props(props)
              .topic(sink.getTopic())
              .semantic(sink.getSemantic(AT_LEAST_ONCE))
              .keySerialization(keySchema)
              .build());
    }

    private KafkaSink<String> buildSink(SinkParams sinkParams) {
        final var rsb = KafkaRecordSerializationSchema.<String>builder()
              .setTopic(sinkParams.getTopic())
              .setValueSerializationSchema(new SimpleStringSchema());

        sinkParams.getKeySerialization().ifPresent(rsb::setKeySerializationSchema);
        return KafkaSink.<String>builder()
              .setBootstrapServers(sinkParams.getProps().getProperty(BOOTSTRAP_SERVERS))
              .setKafkaProducerConfig(sinkParams.getProps())
              .setRecordSerializer(rsb.build())
              .setDeliverGuarantee(DeliveryGuarantee.valueOf(sinkParams.getSemantic()))
              .build();
    }

}
