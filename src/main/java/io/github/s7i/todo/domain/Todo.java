package io.github.s7i.todo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@NoArgsConstructor
public class Todo {

    @SneakyThrows
    public static Todo from(String string) {
        return new ObjectMapper().readValue(string, Todo.class);
    }

    String id;
    List<String> items;

    public Todo(String key) {
        id = key;
        items = new ArrayList<>();
    }

    public void update(TodoAction action) {
        if (action.hasAdd()) {
            items.add(action.getAdd());
        }
        if (action.hasRemove()) {
            items.remove(action.getRemove());
        }
    }

    @SneakyThrows
    public String toJsonString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
