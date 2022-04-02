package io.github.s7i.todo;

import io.github.s7i.todo.conf.Configuration;
import io.github.s7i.todo.conf.Configuration.Checkpoints;
import io.github.s7i.todo.conf.FlinkConfigAdapter;
import io.github.s7i.todo.conf.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class TodoJob {

    public static final OutputTag<String> TAG_TX_LOG = new OutputTag<>("TXLOG", BasicTypeInfo.STRING_TYPE_INFO);
    public static final String PARAM_CONFIG = "config";
    public static final String ENV_CONFIG = "CONFIG";

    @RequiredArgsConstructor
    public static class JobCreator implements FlinkConfigAdapter {

        final StreamExecutionEnvironment env;
        ParameterTool params;
        Configuration cfg;

        void create(String[] args) throws Exception {
            params = ParameterTool.fromArgs(args);
            cfg = getConfiguration();
            requireNonNull(params);
            requireNonNull(cfg);
            requireNonNull(env);
            buildStream();
        }

        @Override
        public List<KafkaTopic> getKafkaTopicList() {
            return cfg.getKafkaTopicList();
        }

        void buildStream() throws Exception {
            var stream = env.fromSource(actionSource(), WatermarkStrategy.noWatermarks(), "Action Source")
                  .filter(new TodoActionFilter())
                  .uid("todo-src")
                  .name("Todo Actions")
                  .keyBy(new TodoKeySelector())
                  .process(new TodoActionProcessor())
                  .setParallelism(params.getInt("scale",2))
                  .name("Todo Processor")
                  .uid("todo-processor");

            stream.getSideOutput(TAG_TX_LOG)
                  .sinkTo(txLog())
                  .name("TxLog")
                  .uid("txlog-sink");

            stream.sinkTo(sink())
                  .name("Todo Reactions")
                  .uid("todo-sink");

            if (cfg.hasCheckpointing()) {
                enableCheckpointing(cfg.getCheckpoints());
            }
            env.execute("ToDo App Job");
        }

        Configuration getConfiguration() throws Exception {
            Configuration cfg;
            if (params.has(PARAM_CONFIG)) {
                var config = params.get(PARAM_CONFIG);
                log.info("Reading config form file: {}", config);
                cfg = Configuration.from(Files.readString(Paths.get(config)));
            } else if (nonNull(System.getenv(ENV_CONFIG))) {
                log.info("Reading config form env");
                cfg = Configuration.from(System.getenv(ENV_CONFIG));
            } else {
                log.info("Reading config resources");
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
