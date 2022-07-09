package io.github.s7i.todo;

import io.github.s7i.todo.domain.TodoAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.accumulators.ListAccumulator;
import org.apache.flink.api.common.accumulators.LongCounter;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;

import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
public class TodoActionFilter extends RichFilterFunction<String> implements CheckpointedFunction {

  public static final ListStateDescriptor<String> KNOWN_DESC =
      new ListStateDescriptor<>("known_correlation", String.class);
  private ListState<String> stateKnownList;
  private List<String> knownList = new LinkedList<>();

  private ListAccumulator<String> knowListAccu = new ListAccumulator<>();
  private LongCounter invalidCounter = new LongCounter();
  private LongCounter alreadyKnownCounter = new LongCounter();

  @Override
  public void open(Configuration parameters) throws Exception {
    getRuntimeContext().addAccumulator("known_list", knowListAccu);
    getRuntimeContext().addAccumulator("invalid_action_counter", invalidCounter);
    getRuntimeContext().addAccumulator("already_known_action_counter", alreadyKnownCounter);
  }

  @Override
  public boolean filter(String s) throws Exception {
    try {
      var correlation = TodoAction.from(s).getContext().get("correlation").asText();

      for (var known : stateKnownList.get()) {
        if (correlation.equals(known)) {
          alreadyKnownCounter.add(1);
          return false;
        }
      }
      knownList.add(correlation);
      knowListAccu.add(correlation);
      return true;

    } catch (Exception e) {
      invalidCounter.add(1);

      log.warn("invalid todo action {}", s);
    }
    return false;
  }

  @Override
  public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
    stateKnownList.clear();

    if (nonNull(knownList)) {
      stateKnownList.addAll(knownList);
    }
  }

  @Override
  public void initializeState(FunctionInitializationContext functionInitializationContext)
      throws Exception {

    stateKnownList =
        functionInitializationContext.getOperatorStateStore().getUnionListState(KNOWN_DESC);
    stateKnownList.get().forEach(this::initKnownEntry);

    log.info("Known list: {}", knownList);
  }

  void initKnownEntry(String correlation) {
    knownList.add(correlation);
    knowListAccu.add(correlation);
  }
}
