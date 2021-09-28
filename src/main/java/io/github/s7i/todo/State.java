package io.github.s7i.todo;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;

public abstract class State {

    public static final ValueStateDescriptor<String> USER_STATE = new ValueStateDescriptor<>("user", BasicTypeInfo.STRING_TYPE_INFO);
    public static final ValueStateDescriptor<String> TODO_STATE = new ValueStateDescriptor<>("todo", BasicTypeInfo.STRING_TYPE_INFO);
}
