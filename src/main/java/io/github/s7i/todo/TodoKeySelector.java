package io.github.s7i.todo;

import static java.util.Objects.isNull;

import io.github.s7i.todo.domain.TodoAction;
import java.util.UUID;
import org.apache.flink.api.java.functions.KeySelector;

public class TodoKeySelector implements KeySelector<String, String> {

    @Override
    public String getKey(String s) throws Exception {
        var action = TodoAction.from(s);
        if (isNull(action.getId())) {
            return UUID.randomUUID().toString();
        }
        return action.getId();
    }
}
