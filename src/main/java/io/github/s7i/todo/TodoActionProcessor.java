package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.Todo;
import io.github.s7i.todo.domain.TodoAction;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

@Slf4j
public class TodoActionProcessor extends KeyedProcessFunction<String, String, String> {

    public static final ValueStateDescriptor<String> TODO_STATE = new ValueStateDescriptor<>("todo", BasicTypeInfo.STRING_TYPE_INFO);

    @Override
    public void processElement(String value, Context context, Collector<String> collector) throws Exception {
        log.info("action: {}", value);
        Todo todo;
        var action = TodoAction.from(value);

        var state = getRuntimeContext().getState(TODO_STATE);
        if (state.value() != null) {
            todo = Todo.from(state.value());
            todo.update(action);

        } else {
            todo = new Todo();
            todo.setId(context.getCurrentKey());
            if (nonNull(action.getAdd())) {
                todo.setItems(List.of(action.getAdd()));
            }
        }
        var jsonString = todo.toJsonString();
        state.update(jsonString);
        collector.collect(jsonString);
    }
}
