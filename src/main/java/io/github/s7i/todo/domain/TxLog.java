package io.github.s7i.todo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TxLog {

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Change {

    Long timestamp;
    TodoAction action;
  }

  @SneakyThrows
  public static TxLog from(String string) {
    return new ObjectMapper().readValue(string, TxLog.class);
  }

  Todo todo;
  List<Change> changeList;

  public TxLog(String key) {
    todo = new Todo(key);
    changeList = new ArrayList<>();
  }

  public void update(TodoAction action) {
    changeList.add(new Change(Instant.now().toEpochMilli(), action));
    todo.update(action);
  }

  @SneakyThrows
  public String toJsonString() {
    return new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .writeValueAsString(this);
  }
}
