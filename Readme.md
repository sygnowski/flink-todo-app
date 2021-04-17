# Flink's Todo Stream Application
### The Todo Actions 
1. Create a Todo List and Add first task:
   `{"add": "My Task #1"}`
2. Add next task to list:
   `{"id": "{todo-list-id}","add": "My Task #2"}`
   


### Miscellaneous

How to create a compacted topic:
`kafka-topics.sh --create --zookeeper zookeeper:2181 --topic TodoTxLog --replication-factor 1 --partitions 1 --config "cleanup.policy=compact" --config "delete.retention.ms=100" --config "segment.ms=100" --config "min.cleanable.dirty.ratio=0.01"`