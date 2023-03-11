package io.github.s7i.todo.conf

import spock.lang.Specification

class ConfigurationTest extends Specification {

    def 'test load form resource'() {
        expect:
        def cfg = Configuration.fromResources()

        cfg.getKafkaTopicList().size() > 0

    }

    def 'source not found'() {
        given:
        FlinkConfigAdapter adapter = { Configuration.fromResources().getKafkaTopicList() }
        when:
        adapter.source("not-existing")
        then:
        thrown(NoSuchElementException.class)
    }
}
