# Flink's Todo Stream Application

In general, this application was written for research purpose.
It's providing functionality that covers various aspects of working with the Flink, like: 
 - Kafka messaging, semantic (exactly once, at most once)
 - Flink's state
 - Checkpointing

## Config

```yaml
kafka-io:
  - name: action
    type: source
    topic: TodoAction
    properties:
      bootstrap.servers: localhost:9092
      group.id: todo-action
  - name: reaction
    type: sink
    topic: TodoReaction
    properties:
      bootstrap.servers: localhost:9092
  - name: txlog
    type: sink
    topic: ActionTxLog
    semantic: EXACTLY_ONCE
    properties:
      bootstrap.servers: localhost:9092
checkpoints:
  enabled: true
  interval: 10000
  mode: AT_LEAST_ONCE
  timeout: 900000
  pause: 1000
  concurrent: 1
  externalization: true
```

## Domain

### The Todo Actions 
1. Create a Todo List and Add first task:
   ```json
   {"add": "My Task #1"}
   ```
2. Add next task to list:
   ```json
   {"id": "{todo-list-id}","add": "My Task #2"}
   ```

## Flink
### Configuration

Enabling RocksDB state

```properties
state.backend: rocksdb
state.checkpoints.dir: file:/opt/flink/appdata/checkpointing
state.savepoints.dir: file:/opt/flink/appdata/savepointing
state.backend.incremental: true
state.backend.rocksdb.timer-service.factory: rocksdb
state.backend.rocksdb.localdir: /opt/flink/appdata/state-rocksdb
```

Flink CLI
- run a application `flink run -d todo-app.jar`

## Miscellaneous
#### Kafka

How to produce a message on TodoAction topic:
```shell
kafka-console-producer.sh --topic TodoAction --bootstrap-server localhost:9092
```

How to create a compacted topic:
```shell
kafka-topics.sh --create --zookeeper zookeeper:2181 --topic TodoTxLog --replication-factor 1 --partitions 1 --config "cleanup.policy=compact" --config "delete.retention.ms=100" --config "segment.ms=100" --config "min.cleanable.dirty.ratio=0.01"
```