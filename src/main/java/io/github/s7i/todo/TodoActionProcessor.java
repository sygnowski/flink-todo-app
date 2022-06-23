package io.github.s7i.todo;

import io.github.s7i.todo.domain.TodoAction;
import io.github.s7i.todo.domain.TxLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

@Slf4j
public class TodoActionProcessor extends KeyedProcessFunction<String, String, String> {

    public static final ValueStateDescriptor<String> TODO_STATE = new ValueStateDescriptor<>("todo", BasicTypeInfo.STRING_TYPE_INFO);

    @Override
    public void processElement(String value, Context context, Collector<String> collector) throws Exception {
        log.info("action: {}", value);

        final TxLog txLog;
        final var action = TodoAction.from(value);

        var state = getRuntimeContext().getState(TODO_STATE);
        if (state.value() != null) {
            txLog = TxLog.from(state.value());
        } else {
            txLog = new TxLog(context.getCurrentKey());
        }

        final var kind = action.getKind();
        log.info("action kind: {}", kind);

        switch (kind) {
            case QUERY:
                break;
            case COMMAND:
                txLog.update(action);

                var txLogJson = txLog.toJsonString();

                state.update(txLogJson);
                context.output(TodoJob.TAG_TX_LOG, txLogJson);
                break;
        }
        collector.collect(txLog.toJsonString());
    }
}
