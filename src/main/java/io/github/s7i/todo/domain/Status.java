package io.github.s7i.todo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.SneakyThrows;

@Data
@Builder
public class Status {

    @Singular
    List<String> errors;

    @SneakyThrows
    public String toJsonString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
