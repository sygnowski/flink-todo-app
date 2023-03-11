#set -ex

KAFKA_BROKER="localhost:9092"

APP_DIR=~/codebase/flink-doto-app
GW_BIN=~/codebase/kafka-gateway/kafka-gateway


function run_gateway() {
    export BROKER=$1
    export PUBLISH_TOPIC=TodoAction
    export SUBSCRIBE_TOPIC=TodoReaction
    export SUBSCRIBE_GROUP_ID=todo-gateway
    export TIMEOUT=10
    ${GW_BIN} > gateway.log 2>&1 &
    echo "$!" > gateway.pid

    echo "Kafka Gateway PID: $!"
}

function run_setup() {
    ${APP_DIR}/flink-cluster/env-setup.sh ${KAFKA_BROKER}
    mvn package -f ${APP_DIR}/pom.xml -DskipTests=true
  }

function run_flink_job() {
    flink run -d ${APP_DIR}/target/todo-app.jar --config ${APP_DIR}/lo-run/cfg.yml 2> /dev/null
}

function run_stop_gateway() {
    kill $(cat gateway.pid)
    rm ./gateway.log
    rm ./gateway.pid
}

function run_clean() {
    run_stop_gateway

    Topics=('TodoAction' 'TodoReaction' 'TodoAdmin' 'TodoTxLog')
    for name in "${Topics[@]}"; do
        kafka-topics.sh --delete --bootstrap-server ${KAFKA_BROKER} --topic $name
    done
}

function usage() {
    (lolcat || cat) << EOF 2> /dev/null

====================================================================
Usage:
gw    - start gateway
gws   - stop gateway
job   - run flink job
setup - initialize (crate Kafka topics, make job jar file)
clean - cleanup (delete Kafka topics)
====================================================================
Current configuration:
APP_DIR ${APP_DIR}
KAFKA_BROKER ${KAFKA_BROKER}
JOB_CONFIG
$(cat ${APP_DIR}/lo-run/cfg.yml)
EOF
}

case $1 in
    gw)
        run_gateway ${KAFKA_BROKER}
        ;;
    gws)
        run_stop_gateway
        ;;
    job)
        run_flink_job
        ;;
    setup)
        run_setup
        ;;
    clean)
        run_clean
        ;;
    *)
    usage
esac
