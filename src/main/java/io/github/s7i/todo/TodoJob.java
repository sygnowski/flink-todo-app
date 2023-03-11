package io.github.s7i.todo;

import io.github.s7i.todo.conf.Configuration;
import io.github.s7i.todo.conf.Configuration.Checkpoints;
import io.github.s7i.todo.conf.FlinkConfigAdapter;
import io.github.s7i.todo.conf.GitProps;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class TodoJob {

  public static final OutputTag<String> TAG_TX_LOG =
      new OutputTag<>("TXLOG", BasicTypeInfo.STRING_TYPE_INFO);
  public static final String PARAM_CONFIG = "config";
  public static final String ENV_CONFIG = "CONFIG";
  public static final MapStateDescriptor<String, String> ADMIN_STREAM_DESCRIPTOR = new MapStateDescriptor<>("admin-broadcast", BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO);

  @RequiredArgsConstructor
  @Accessors(fluent = true)
  public static class JobCreator {

    final StreamExecutionEnvironment env;
    ParameterTool params;
    Configuration cfg;
    @Setter
    FlinkConfigAdapter configAdapter = () -> cfg.getKafkaTopicList();

    public void create(String[] args) throws Exception {
      params = ParameterTool.fromArgs(args);
      cfg = getConfiguration();
      requireNonNull(params);
      requireNonNull(cfg);
      requireNonNull(env);
      buildStream();
    }

    void buildStream() throws Exception {
      WatermarkStrategy<String> wms = WatermarkStrategy.forMonotonousTimestamps();

      var srcStream = configAdapter.buildSourceStream(env, wms, FlinkConfigAdapter.ACTION)
              .uid("todo-src");

      if (params.has("src-scale")) {
        srcStream.setParallelism(params.getInt("src-scale"));
      }

      var adminStream = configAdapter.buildSourceStream(env, wms, FlinkConfigAdapter.ADMIN)
              .uid("admin-src")
              .broadcast(ADMIN_STREAM_DESCRIPTOR);

      var todoStream =
          srcStream
              .filter(new TodoActionFilter())
              .uid("todo-act-flt")
              .name("Todo Action Filter")
              .keyBy(new TodoKeySelector())
              .connect(adminStream)
              .process(new Buffer())
              .name("Buffer")
              .uid("buffer")
              .keyBy(new TodoKeySelector())
              .process(new TodoActionProcessor())
              .name("Todo Processor")
              .uid("todo-processor");

      if (params.has("scale")) {
        todoStream.setParallelism(params.getInt("scale"));
      }

      todoStream.getSideOutput(TAG_TX_LOG).sinkTo(configAdapter.txLog()).name("TxLog").uid("txlog-sink");

      todoStream.sinkTo(configAdapter.sinkOfReaction()).name("Todo Reactions").uid("todo-sink");

      if (cfg.hasCheckpointing()) {
        enableCheckpointing(cfg.getCheckpoints());
      }
      env.getConfig().setGlobalJobParameters(params);
      env.execute(jobName());
    }

    private String jobName() {
      return String.format("Todo App (%s)", new GitProps());
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

        var ecc = ExternalizedCheckpointCleanup.valueOf(config.getExternalization());

        chk.setExternalizedCheckpointCleanup(ecc);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    var env = StreamExecutionEnvironment.getExecutionEnvironment();
    new JobCreator(env).create(args);
  }
}
