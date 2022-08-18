package io.github.s7i.todo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import static java.util.Objects.nonNull;

@Data
@NoArgsConstructor
public class TodoAction {

  @SneakyThrows
  public static TodoAction from(String string) {
    return new ObjectMapper().readValue(string, TodoAction.class);
  }

    String id;
    String name;
    String add;
    String remove;
    List<Meta> meta;

    public Optional<List<Meta>> getMeta() {
        return Optional.ofNullable(meta);
    }

  JsonNode context;

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
