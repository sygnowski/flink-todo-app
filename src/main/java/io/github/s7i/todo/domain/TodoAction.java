package io.github.s7i.todo.domain;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@NoArgsConstructor
public class TodoAction {

    @SneakyThrows
    public static TodoAction from(String string) {
        return new ObjectMapper().readValue(string, TodoAction.class);
    }

    String id;
    String add;
    String remove;
    User user;
    List<String> flags = new ArrayList<>();

    public boolean hasAdd() {
        return nonNull(add);
    }

    public boolean hasRemove() {
        return nonNull(remove);
    }

    @SneakyThrows
    public String toJsonString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
