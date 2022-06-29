package io.github.s7i.todo.conf;

import static java.util.Objects.requireNonNull;

import io.github.s7i.todo.domain.TxLog;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

public class TxLogKeySerializer extends SimpleStringSchema {

    @Override
    public byte[] serialize(String element) {
        var log = TxLog.from(element);
        final var id = log.getTodo().getId();
        requireNonNull(id);
        return super.serialize(id);
    }
}
