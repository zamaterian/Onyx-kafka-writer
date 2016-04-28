#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

echo "Starting batch $APP_NAME"
jar_file=`find / -name "*-standalone.jar"|head -1`
echo $jar_file
# when running in container, the host is the container name
mkdir -p /logs/$HOSTNAME

cd /logs/$HOSTNAME
echo "PWD $PWD"

echo "Setting shared memory for Aeron"

mount -t tmpfs -o remount,rw,nosuid,nodev,noexec,relatime,size=256M tmpfs /dev/shm

exec java ${MEDIA_DRIVER_JAVA_OPTS:-} -DappName=$APP_NAME -Duser.dir=/logs/$HOSTNAME -server -cp $jar_file "batch_vur.aeron_media_driver" >> ./aeron.log 2>&1
