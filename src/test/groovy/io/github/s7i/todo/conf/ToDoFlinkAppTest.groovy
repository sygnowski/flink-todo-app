package io.github.s7i.todo.conf

import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration
import org.apache.flink.streaming.util.TestStreamEnvironment
import org.apache.flink.test.util.MiniClusterWithClientResource
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class ToDoFlinkAppTest extends Specification {

    @ClassRule
    @Shared
    MiniClusterWithClientResource flinkCluster = new MiniClusterWithClientResource(
            new MiniClusterResourceConfiguration.Builder()
                    .setNumberSlotsPerTaskManager(1)
                    .setNumberTaskManagers(1)
                    .build())

    def "stream test"() {

        given:
        def env = TestStreamEnvironment.getExecutionEnvironment()

        expect:
        env

    }
}
