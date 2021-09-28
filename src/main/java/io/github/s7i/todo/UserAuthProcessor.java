package io.github.s7i.todo;

import static java.util.Objects.nonNull;

import io.github.s7i.todo.domain.TodoAction;
import io.github.s7i.todo.domain.User;
import lombok.SneakyThrows;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class UserAuthProcessor extends KeyedProcessFunction<String, String, String> {

    @Override
    public void processElement(String action, Context context, Collector<String> collector) throws Exception {
        var act = TodoAction.from(action);
        if (nonNull(act.getUser())) {
            var userState = getRuntimeContext().getState(State.USER_STATE).value();

            if (nonNull(userState)) {
                authExistingUser(act, userState);
            } else {
                addNewUser(act.getUser());
            }
        }
        collector.collect(act.toJsonString());
    }

    @SneakyThrows
    private void addNewUser(User user) {
        getRuntimeContext().getState(State.USER_STATE).update(user.asJsonString());
    }

    private void authExistingUser(TodoAction act, String userState) {
        var storedUser = User.from(userState);

        var authOK = storedUser.getPswdHash().equals(act.getUser().getPswdHash());
        act.getFlags().add(authOK ? "auth.passed" : "auth.failed");
    }
}
