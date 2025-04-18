#!/usr/bin/env bash
set -eux
set -o pipefail
cd $(dirname "${BASH_SOURCE[0]}")

./mvnw clean install -DskipTests
keycloak_version=$(./get-version.sh)
cp -f "./quarkus/dist/target/keycloak-${keycloak_version}.tar.gz" ./quarkus/container/
docker build -t "mapletree/dalaran-keycloak:${keycloak_version}" \
  --build-arg KEYCLOAK_DIST="keycloak-${keycloak_version}.tar.gz" \
  -f ./quarkus/container/Dockerfile ./quarkus/container/

