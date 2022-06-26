package io.github.s7i.todo.domain;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class TodoAction {

    @SneakyThrows
    public static TodoAction from(String string) {
        return new ObjectMapper().readValue(string, TodoAction.class);
    }

    String id;
    String add;
    String remove;

    JsonNode context;

    public boolean hasAdd() {
        return nonNull(add);
    }

    public boolean hasRemove() {
        return nonNull(remove);
    }

    @JsonIgnore
    public Kind getKind() {
        return hasAdd() || hasRemove()
              ? Kind.COMMAND
              : Kind.QUERY;
    }

}
