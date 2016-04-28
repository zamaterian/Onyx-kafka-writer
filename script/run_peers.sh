#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o xtrace

export BIND_ADDR="${BIND_ADDR:-$(hostname --ip-address)}"

echo "Starting batch $APP_NAME"
jar_file=`find / -name "*-standalone.jar"|head -1`
echo $jar_file
# when running in container, the host is the container name
mkdir -p /logs/$HOSTNAME

cd /logs/$HOSTNAME
echo "PWD $PWD"

exec java ${PEER_JAVA_OPTS:-} -DappName=$APP_NAME -Duser.dir=/logs/$HOSTNAME -server  -jar $jar_file
