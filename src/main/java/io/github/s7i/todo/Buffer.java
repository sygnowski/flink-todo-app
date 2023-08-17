package io.github.s7i.todo;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.accumulators.ListAccumulator;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.state.KeyedStateFunction;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.FlinkRuntimeException;

import java.util.stream.StreamSupport;

@Slf4j
public class Buffer extends KeyedBroadcastProcessFunction<String, String, String, String> implements CheckpointedFunction {

    public static final ListStateDescriptor<String> DESCRIPTOR = new ListStateDescriptor<>("buffer", BasicTypeInfo.STRING_TYPE_INFO);
    public static final ListStateDescriptor<String> OPERATOR_STATE_DESC = new ListStateDescriptor<>("operator_buffer", BasicTypeInfo.STRING_TYPE_INFO);
    private ListState<String> bufferList;
    private ListState<String> operatorState;
    private ListAccumulator<String> bufferState = new ListAccumulator<>();

    @Override
    public void open(Configuration parameters) throws Exception {
        bufferList = getRuntimeContext().getListState(DESCRIPTOR);
        getRuntimeContext().addAccumulator("buffer_state", bufferState);
    }

    @Override
    public void processElement(String input, KeyedBroadcastProcessFunction<String, String, String, String>.ReadOnlyContext readOnlyContext, Collector<String> collector) throws Exception {
        var adminOption = readOnlyContext.getBroadcastState(TodoJob.ADMIN_STREAM_DESCRIPTOR).get("admin");

        if ("hold".equals(adminOption)) {
            bufferList.add(input);
            log.info("adding into buffer: {}", input);

        } else if ("nothing".equals(adminOption)) {
            log.info("DOING NOTHING with: {}", input);
            getRuntimeContext().getLongCounter("do_nothing").add(1);

        } else {
            collector.collect(input);
        }

        bufferState.resetLocal();
        bufferState.add("buffer.option=" + adminOption);
        bufferState.add("buffer.size=" + count(bufferList.get()));
    }

    @Override
    public void processBroadcastElement(String adminMessage, KeyedBroadcastProcessFunction<String, String, String, String>.Context context, Collector<String> collector) throws Exception {
        BroadcastState<String, String> broadcastState = context.getBroadcastState(TodoJob.ADMIN_STREAM_DESCRIPTOR);

        log.info("getting broadcast: {}", adminMessage);

        context.getBroadcastState(TodoJob.ADMIN_STREAM_DESCRIPTOR).put("admin", adminMessage);

        switch (adminMessage) {
            case "free":
                context.applyToKeyedState(DESCRIPTOR, new KeyedStateFunction<String, ListState<String>>() {
                    @Override
                    public void process(String key, ListState<String> stringListState) throws Exception {
                        log.info("apply broadcast on key: {}", key);
                        for (var fromState : stringListState.get()) {
                            collector.collect(fromState);
                        }
                        stringListState.clear();
                    }
                });
                break;
            case "prt-op-st":
                operatorState.get().forEach(e -> log.info("OP State: {}", e));
                break;
            default:
                log.warn("unknown admin op: {}", adminMessage);
                break;
        }
    }

    private static long count(Iterable<?> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).count();
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        operatorState.clear();

        bufferState.getLocalValue().forEach(e -> {
            try {
                operatorState.add(e);
            } catch (Exception ex) {
                log.error("oops", ex);
                throw new FlinkRuntimeException(ex);
            }
        });


    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {
        operatorState = functionInitializationContext.getOperatorStateStore().getUnionListState(OPERATOR_STATE_DESC);

    }
}
