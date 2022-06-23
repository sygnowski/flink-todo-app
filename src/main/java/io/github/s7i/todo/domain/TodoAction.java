package io.github.s7i.todo.domain;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    List<Meta> meta;

    public boolean hasAdd() {
        return nonNull(add);
    }

    public boolean hasRemove() {
        return nonNull(remove);
    }

    public Kind getKind() {
        return hasAdd() || hasRemove()
              ? Kind.COMMAND
              : Kind.QUERY;
    }

}
