#!/bin/bash -e
set -ex

KAFKA=kafka:9093
RETENTION=60000

create_topic() {
    local TOPIC_NAME=$1

    kafka-topics.sh --create \
        --if-not-exists \
        --bootstrap-server ${KAFKA} \
        --topic ${TOPIC_NAME} \
        --replication-factor 1 \
        --partitions 1 \
        --config "retention.ms=${RETENTION}"
}

create_topic_compact() {
    local TOPIC_NAME=$1

    kafka-topics.sh --create \
        --if-not-exists \
        --bootstrap-server ${KAFKA} \
        --topic ${TOPIC_NAME} \
        --replication-factor 1 \
        --partitions 1 \
        --config "cleanup.policy=compact" \
        --config "delete.retention.ms=10000" \
        --config "segment.ms=10000" \
        --config "min.cleanable.dirty.ratio=0.01"
}

create_topic TodoAction
create_topic TodoReaction
create_topic_compact TodoTxLog
