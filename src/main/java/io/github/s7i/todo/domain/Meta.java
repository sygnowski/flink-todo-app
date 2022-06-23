package io.github.s7i.todo.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Meta {
    String key;
    String value;
    Long timestamp;
}
