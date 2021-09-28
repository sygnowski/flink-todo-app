package io.github.s7i.todo;

import io.github.s7i.todo.domain.TodoAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FilterFunction;

@Slf4j
public class TodoActionFilter implements FilterFunction<String> {

    @Override
    public boolean filter(String s) throws Exception {
        try {
            TodoAction.from(s);
            return true;
        } catch (Exception e) {
            log.warn("invalid todo action {}, why {}", s, e.getMessage());

        }
        return false;
    }
}
