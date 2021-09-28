package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.StateSpy;
import io.github.s7i.todo.domain.TodoAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

@Slf4j
public class StateSpyProcessor extends KeyedProcessFunction<String, String, String> {

    @Override
    public void processElement(String data, Context context, Collector<String> collector) throws Exception {
        var act = TodoAction.from(data);
        StateSpy spy;
        if ((spy = act.getSpy()) != null) {

            if (nonNull(spy.getKeyUser())) {
                var userStateValue = getRuntimeContext().getState(State.USER_STATE).value();
                log.info("User state key: {}, value {}", context.getCurrentKey(), userStateValue);
            }

            if (nonNull(spy.getKeyTodo())) {
                var todoStateValue = getRuntimeContext().getState(State.TODO_STATE).value();
                log.info("Todo List state key:{} , value {}", context.getCurrentKey(), todoStateValue);
            }
        }
        collector.collect(data);
    }
}
