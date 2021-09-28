package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.Status;
import io.github.s7i.todo.domain.TodoAction;
import io.github.s7i.todo.domain.TxLog;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

@Slf4j
public class TodoActionProcessor extends KeyedProcessFunction<String, String, String> {

    public static final String AUTH_FAILED = "auth.failed";

    @Override
    public void processElement(String value, Context context, Collector<String> collector) throws Exception {
        log.info("action: {}", value);

        var action = TodoAction.from(value);

        if (action.getFlags().contains(AUTH_FAILED)) {
            collector.collect(Status.builder()
                  .error(AUTH_FAILED)
                  .build()
                  .toJsonString());
            return;
        }

        if (nonNull(action.getAdd()) || nonNull(action.getRemove())) {

            var state = getRuntimeContext().getState(State.TODO_STATE);
            TxLog txLog = txLog(context, state);
            txLog.update(action);

            var todoJson = txLog.getTodo().toJsonString();

            var txLogJson = txLog.toJsonString();
            state.update(txLogJson);
            context.output(TodoJob.TAG_TX_LOG, txLogJson);

            collector.collect(todoJson);
        } else if (nonNull(action.getSpy())) {
            collector.collect(Status.builder()
                  .status("spy")
                  .currentKey(context.getCurrentKey())
                  .build()
                  .toJsonString());
        }
    }

    private TxLog txLog(Context context, ValueState<String> state) throws IOException {
        TxLog txLog;
        if (state.value() != null) {
            txLog = TxLog.from(state.value());
        } else {
            txLog = new TxLog(context.getCurrentKey());

        }
        return txLog;
    }
}
