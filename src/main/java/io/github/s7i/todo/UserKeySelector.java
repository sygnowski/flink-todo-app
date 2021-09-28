package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.TodoAction;
import org.apache.flink.api.java.functions.KeySelector;

public class UserKeySelector implements KeySelector<String, String> {

    @Override
    public String getKey(String s) throws Exception {
        var user = TodoAction.from(s).getUser();
        if (nonNull(user)) {
            return user.getName();
        }

        return "none";
    }
}
