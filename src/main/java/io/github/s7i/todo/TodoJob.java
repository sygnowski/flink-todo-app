package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.conf.Configuration;
import io.github.s7i.todo.conf.Configuration.Checkpoints;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

public class TodoJob {

    public static final OutputTag<String> TAG_TX_LOG = new OutputTag<>("TXLOG", BasicTypeInfo.STRING_TYPE_INFO);

    public static void main(String[] args) throws Exception {
        var params = ParameterTool.fromArgs(args);
        var cfg = getConfiguration();

        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        var stream = env.addSource(cfg.actionSource())
              .filter(new TodoActionFilter())
              .uid("todo-src")
              .name("Todo Actions")
              .keyBy(new TodoKeySelector())
              .process(new TodoActionProcessor())
              .name("Todo Processor")
              .uid("todo-processor");

        stream.getSideOutput(TAG_TX_LOG)
              .addSink(cfg.txLog())
              .name("TxLog")
              .uid("txlog-sink");

        stream.addSink(cfg.sink())
              .name("Todo Reactions")
              .uid("todo-sink");

        if (cfg.hasCheckpointing()) {
            enableCheckpointing(env, cfg.getCheckpoints());
        }

        env.execute("ToDo App Job");
    }

    private static Configuration getConfiguration() {
        Configuration cfg;
        if (nonNull(System.getenv("CONFIG"))) {
            cfg = Configuration.from(System.getenv("CONFIG"));
        } else {
            cfg = Configuration.fromResources();
        }
        return cfg;
    }

    private static void enableCheckpointing(StreamExecutionEnvironment env, Checkpoints config) {
        if (config.isEnabled()) {
            var chk = env.getCheckpointConfig();
            chk.setCheckpointingMode(CheckpointingMode.valueOf(config.getMode()));
            chk.setCheckpointTimeout(config.getTimeout());
            chk.setCheckpointInterval(config.getInterval());
            chk.setMinPauseBetweenCheckpoints(config.getPause());
            chk.setMaxConcurrentCheckpoints(config.getConcurrent());
            if (config.isExternalization()) {
                chk.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
            }
        }
    }
}
