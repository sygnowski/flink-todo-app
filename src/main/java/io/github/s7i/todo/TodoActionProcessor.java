package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.Meta;
import io.github.s7i.todo.domain.Prime;
import io.github.s7i.todo.domain.TodoAction;
import io.github.s7i.todo.domain.TxLog;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
            if (nonNull(action.getName())) {
                txLog.setName(action.getName());
            }
        }

        final var kind = action.getKind();
        log.info("action kind: {}", kind);

        List<Meta> metaList = new LinkedList<>();
        action.getMeta().ifPresent(metaList::addAll);

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

        var opt = metaList.stream()
              .filter(meta -> meta.getKey().equals("sleep"))
              .findFirst();

        if (opt.isPresent()) {
            try {
                final var timeout = Long.parseLong(opt.get().getValue());
                log.info("going to sleep for {} ms", timeout);
                TimeUnit.MILLISECONDS.sleep(timeout);
            } catch (Exception e) {
                log.error("sleep", e);
            }
        }

        var prime = metaList.stream()
              .filter(meta -> meta.getKey().equals("prime"))
              .findFirst();

        if (prime.isPresent()) {
            try {
                final var numOfP = Integer.parseInt(prime.get().getValue());
                log.info("gen {} prime numbers", numOfP);
                Prime.runStressPrime(numOfP);
            } catch (Exception e) {
                log.error("sleep", e);
            }
        }

        metaList.add(Meta.builder()
              .key("operator.name")
              .value(getRuntimeContext().getTaskName())
              .build());

        metaList.add(Meta.builder()
              .key("operator.nameWithSubtasks")
              .value(getRuntimeContext().getTaskNameWithSubtasks())
              .build());

        txLog.setMeta(metaList);

        collector.collect(txLog.toJsonString());
    }
}
