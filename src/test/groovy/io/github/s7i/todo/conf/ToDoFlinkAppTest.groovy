package io.github.s7i.todo.conf

import io.github.s7i.todo.TodoJob
import io.github.s7i.todo.domain.Todo
import io.github.s7i.todo.domain.TxLog
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
    public static final String TX_LOG = "txLog"
    public static final String REACTION = "reaction"
    @ClassRule
    @Shared
    MiniClusterWithClientResource flinkCluster = new MiniClusterWithClientResource(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(1)
                    .setNumberTaskManagers(1)
                    .build())

    class TestConfigAdapter implements FlinkConfigAdapter {

        def actions

        TestConfigAdapter(actions) {
            this.actions = actions
        }

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
            new Sink2(ToDoFlinkAppTest.TX_LOG)
        }

        @Override
        Sink<String> sinkOfReaction() {
            new Sink2(ToDoFlinkAppTest.REACTION)
        }
    }

    static class Sink2 implements SinkWriter<String>, Sink<String> {
        String sinkName

        Sink2(name) {
            this.sinkName = name
            sinks[sinkName] = []
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
        {
          "id": "test-list",
          "add": "My Task #1",
          "context": {
            "correlation": "1"
          }
        }
'''
    //language=json
    String actionAdd02 = '''
        {
          "id": "test-list",
          "add": "My Task #2",
          "context": {
            "correlation": "1"
          }
        }
'''
    //language=json
    String actionAdd03 = '''
        {
          "id": "test-list",
          "add": "My Task #3",
          "context": {
            "correlation": "1"
          }
        }
'''

    def "Todo List with 3 entries"() {

        given:
        def fca = new TestConfigAdapter([
                actionAdd01,
                actionAdd02,
                actionAdd03
        ])
        def args = new String[]{"--test", "true"}
        def env = TestStreamEnvironment.getExecutionEnvironment()
        new TodoJob.JobCreator(env).configAdapter(fca).create(args)

        expect:
        def txs = sinks[TX_LOG] as List
        def reactions = sinks[REACTION] as List

        reactions.size() == 3
        txs.size() == 3

        TxLog.from(txs.last()).getTodo().getItems().containsAll(["My Task #1", "My Task #2", "My Task #3"])
        Todo.from(reactions.last()).getItems().containsAll(["My Task #1", "My Task #2", "My Task #3"])


        println(sinks)
    }
}
