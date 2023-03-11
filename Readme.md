# Flink's Todo Stream Application

## Motivation

In general, this application was written for research and learning purposes.
It's providing functionality that covers various aspects of working with the Flink, like: 
 - Flink stream processing
 - Kafka messaging, semantic (exactly once, at most once)
 - Flink's state
 - Checkpointing
 - State restoring

Current Flink version: `1.15.2`

## Deployment
In order to run the application you need to deploy the Kafka first. Broker should have listener setup at `kafka:9093`.

Docker compose manifest under `flink-cluster` directory with more details.

Used Custom Components:
- Kafka Gateway [s7i/kafka-gateway](https://github.com/sygnowski/kafka-gateway)
- Flink Standalone Cluster [s7i/flink](https://github.com/nefro85/dev-images/tree/main/flink)

## API and domain operations

Application interface:
- Endpoint: http://localhost:8180/
- Web-Method: `POST`
- Payload: `json`


###
### The Todo Actions 
- Query for Todo List:
   ```json
   {"id": "{todo-list-id}"}
   ```
- Create a Todo List and Add first task:
   ```json
   {"add": "My Task #1"}
   ```
- Add next task to the list:
   ```json
   {"id": "{todo-list-id}","add": "My Task #2"}
   ```
- Remove task from the list:
   ```json
   {"id": "{todo-list-id}","remove": "My Task #1"}
   ```


## Application configuration

The configuration could be provided via:
- Environment variable `CONFIG`
- Flink's job parameter `--config`



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
```


## Flink

### API
- Find a last completed checkpoint:
  ```
  curl http://localhost:8081/jobs/${JOB_ID}/checkpoints --silent | jq -r '.latest .completed .external_path'
  ```

### Configuration

- Enabling RocksDB state

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
kafka-topics.sh --create \
--zookeeper zookeeper:2181 \
--topic TodoTxLog \
--replication-factor 1 \
--partitions 1 \
--config "cleanup.policy=compact" \
--config "delete.retention.ms=100" \
--config "segment.ms=100" \
--config "min.cleanable.dirty.ratio=0.01"

```