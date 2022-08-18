package io.github.s7i.todo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meta {
    String key;
    String value;
    @Default
    Long timestamp = System.currentTimeMillis();
}
