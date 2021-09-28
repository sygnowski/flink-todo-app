package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.StateSpy;
import io.github.s7i.todo.domain.TodoAction;
import org.apache.flink.api.java.functions.KeySelector;

public class StateSpyKeySelector implements KeySelector<String, String> {

    @Override
    public String getKey(String s) throws Exception {
        var act = TodoAction.from(s);
        StateSpy spy;
        if ((spy = act.getSpy()) != null) {

            if (nonNull(spy.getKeyUser())) {
                return spy.getKeyUser();
            }

            if (nonNull(spy.getKeyTodo())) {
                return spy.getKeyTodo();
            }
        }
        return "none";
    }
}
