#! /bin/sh

mkdir -p $CODECC_LOGS_DIR
chmod 777 $CODECC_LOGS_DIR

XMS=${JVM_XMS:-"512m"}
XMX=${JVM_XMX:-"1024m"}
CODECC_JVM_OPTION="-Xms${XMS} -Xmx${XMX} ${JVM_OPTIONS}"

java -server \
     -Dsun.jnu.encoding=UTF-8 \
     -Dfile.encoding=UTF-8 \
     -Xss512k \
     -XX:+UseG1GC \
     -XX:+UnlockExperimentalVMOptions \
     -XX:G1NewSizePercent=60 \
     -XX:G1MaxNewSizePercent=60 \
     -XX:MaxGCPauseMillis=800 \
     -Xloggc:$CODECC_LOGS_DIR/gc.log \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=oom.hprof \
     -XX:ErrorFile=$CODECC_LOGS_DIR/error_sys.log \
     -Dspring.profiles.active=$CODECC_PROFILE \
     -Dserver.prefix=$SERVICE_PREFIX \
     -Dserver.namespace=$NAMESPACE \
     -Dserver.fullname=${SERVICE_PREFIX}${MS_NAME} \
     -Dserver.common.name=${SERVICE_PREFIX}common \
     -Dservice.log.dir=$CODECC_LOGS_DIR/ \
     -Dcodecc.storage.mouth.path=$SERVER_STORAGE_MOUNT_PATH \
     $CODECC_JVM_OPTION \
     -jar /data/workspace/app.jar