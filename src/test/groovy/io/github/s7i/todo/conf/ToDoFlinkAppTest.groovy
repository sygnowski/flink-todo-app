package io.github.s7i.todo.conf

import io.github.s7i.todo.TodoJob
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.connector.sink2.Sink
import org.apache.flink.api.connector.sink2.SinkWriter
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.util.TestStreamEnvironment
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class ToDoFlinkAppTest extends Specification {

    static sinks = [:]
    @ClassRule
    @Shared
    MiniClusterWithClientResource flinkCluster = new MiniClusterWithClientResource(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(1)
                    .setNumberTaskManagers(1)
                    .build())

    class TestConfigAdapter implements FlinkConfigAdapter {

        def actions = [actionAdd01]
        @Override
        List<KafkaTopic> getKafkaTopicList() {
            return Configuration.fromResources().getKafkaTopicList()
        }

        @Override
        SingleOutputStreamOperator<String> buildSourceStream(StreamExecutionEnvironment env, WatermarkStrategy<String> wms, String sourceName) {
            return env.fromCollection(actions)
        }

        @Override
        Sink<String> txLog() {
            new Sink2("txLog")
        }

        @Override
        Sink<String> sinkOfReaction() {
            new Sink2("reaction")
        }
    }

    class Sink2 implements SinkWriter<String>, Sink<String>{
        String sinkName

        Sink2(name) {
            this.sinkName = name
        }

        @Override
        SinkWriter<String> createWriter(InitContext context) throws IOException {
            return this
        }

        @Override
        void write(String element, Context context) throws IOException, InterruptedException {
            sinks[sinkName] << element
        }

        @Override
        void flush(boolean endOfInput) throws IOException, InterruptedException {

        }

        @Override
        void close() throws Exception {

        }
    }

    //language=json
    String actionAdd01 = '''
        {"id": "test-list","add": "My Task #1"}
'''

    def "stream test"() {

        given:
        def fca = new TestConfigAdapter()
        def args = new String[0]
        def env = TestStreamEnvironment.getExecutionEnvironment()
        new TodoJob.JobCreator(env).configAdapter(fca).create(args)

        expect:
        env

    }
}
