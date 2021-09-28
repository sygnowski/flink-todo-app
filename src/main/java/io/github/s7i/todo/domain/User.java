package io.github.s7i.todo.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@NoArgsConstructor
public class User {

    @SneakyThrows
    public static User from(String json) {
        return new ObjectMapper().readValue(json, User.class);
    }

    String name;
    String pswdHash;

    @SneakyThrows
    public String asJsonString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
