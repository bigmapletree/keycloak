#!/bin/bash
set -eux
set -o pipefail
cd $(dirname "${BASH_SOURCE[0]}")/..

build=${1:-""}

if [[ ! -z "${build}" ]]; then
    ./mvnw -pl quarkus/dist,quarkus/deployment -am -DskipTests clean install
fi

set +x
ALIYUN_ACCESS_KEY_ID=$(<"${HOME}/mapletree/secrets/dalaran/ALIYUN_ACCESS_KEY_ID")
ALIYUN_ACCESS_KEY_SECRET=$(<"${HOME}/mapletree/secrets/dalaran/ALIYUN_ACCESS_KEY_SECRET")
set -x

java -jar quarkus/server/target/lib/quarkus-run.jar start-dev \
    --spi-phone-default-service=aliyun \
    --spi-message-sender-service-aliyun-key ${ALIYUN_ACCESS_KEY_ID} \
    --spi-message-sender-service-aliyun-secret ${ALIYUN_ACCESS_KEY_SECRET} \
    --spi-message-sender-service-aliyun-auth-template SMS_154950909 \
    --spi-message-sender-service-aliyun-registration-template SMS_154950909 \
    --spi-message-sender-service-aliyun-sign "阿里云短信测试"
