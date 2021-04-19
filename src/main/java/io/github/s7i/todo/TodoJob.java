package io.github.s7i.todo;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import io.github.s7i.todo.conf.Configuration;
import io.github.s7i.todo.conf.Configuration.Checkpoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

@Slf4j
public class TodoJob {

    public static final OutputTag<String> TAG_TX_LOG = new OutputTag<>("TXLOG", BasicTypeInfo.STRING_TYPE_INFO);
    public static final String AT_LEAST_ONCE = "AT_LEAST_ONCE";
    public static final String PARAM_SEMANTIC = "semantic";

    @RequiredArgsConstructor
    public static class JobCreator {

        final StreamExecutionEnvironment env;
        ParameterTool params;
        Configuration cfg;
        String semantic = AT_LEAST_ONCE;

        void create(String[] args) throws Exception {
            params = ParameterTool.fromArgs(args);
            if (params.has(PARAM_SEMANTIC)) {
                semantic = params.get(PARAM_SEMANTIC);
            }
            cfg = getConfiguration();
            requireNonNull(params);
            requireNonNull(cfg);
            requireNonNull(env);
            buildStream();
        }

        void buildStream() throws Exception {
            var stream = env.addSource(cfg.actionSource())
                  .filter(new TodoActionFilter())
                  .uid("todo-src")
                  .name("Todo Actions")
                  .keyBy(new TodoKeySelector())
                  .process(new TodoActionProcessor())
                  .name("Todo Processor")
                  .uid("todo-processor");

            stream.getSideOutput(TAG_TX_LOG)
                  .addSink(cfg.txLog(semantic))
                  .name("TxLog")
                  .uid("txlog-sink");

            stream.addSink(cfg.sink(semantic))
                  .name("Todo Reactions")
                  .uid("todo-sink");

            if (cfg.hasCheckpointing()) {
                enableCheckpointing(cfg.getCheckpoints());
            }
            env.execute("ToDo App Job");
            log.info("Flink Producer Semantic: {}", semantic);
        }

        Configuration getConfiguration() {
            Configuration cfg;
            if (nonNull(System.getenv("CONFIG"))) {
                cfg = Configuration.from(System.getenv("CONFIG"));
            } else {
                cfg = Configuration.fromResources();
            }
            return cfg;
        }

        void enableCheckpointing(Checkpoints config) {
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

    public static void main(String[] args) throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        new JobCreator(env).create(args);
    }
}
