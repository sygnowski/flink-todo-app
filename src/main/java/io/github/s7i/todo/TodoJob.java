package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.conf.Configuration;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TodoJob {

    public static void main(String[] args) throws Exception {
        var params = ParameterTool.fromArgs(args);
        Configuration cfg;
        if (nonNull(System.getenv("CONFIG"))) {
            cfg = Configuration.from(System.getenv("CONFIG"));
        } else {
            cfg = Configuration.fromResources();
        }

        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.addSource(cfg.actionSource())
              .uid("todo-src")
              .name("Todo Actions")
              .keyBy(new TodoKeySelector())
              .process(new TodoActionProcessor())
              .name("Todo Processor")
              .uid("todo-processor")
              .addSink(cfg.sink())
              .name("Todo Reactions")
              .uid("todo-sink");

        env.execute("ToDo App Job");

    }
}
