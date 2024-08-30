FROM bkci/jdk:0.0.5

LABEL maintainer="Tencent BlueKing Devops"

ENV CODECC_HOME=/data/workspace \
    CODECC_LOGS_DIR=/data/workspace/logs \
    CODECC_LOGS_LEVEL=INFO \
    SERVICE_PREFIX=codecc \
    CODECC_PROFILE=native \
    NAMESPACE=codecc \
    MS_NAME=codecc \
    SERVER_STORAGE_MOUNT_PATH=/data/workspace/nfs


COPY tmp/ /data/workspace/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/workspace/startup.sh
WORKDIR /data/workspace
ENTRYPOINT [ "sh", "/data/workspace/startup.sh" ]