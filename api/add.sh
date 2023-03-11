set -ex
LIST_ID=$1
LIST_ITEM=$2

API_GATEWAY="http://localhost:8080/api"

payload() {
  cat <<EOF
  {"id": "${LIST_ID}", "add": "${LIST_ITEM}"}
EOF
}


curl -X POST -H "Content-Type: application/json" \
    -d "$(payload)" \
    ${API_GATEWAY}
