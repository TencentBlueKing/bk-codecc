FROM mirrors.tencent.com/ruitaoyuan/mongo-shell:0.0.1

LABEL maintainer="blueking"

COPY ./ /data/workspace/

RUN chmod +x /data/workspace/init-entrance.sh
RUN chmod +x /data/workspace/bk-ci-gen-jwt-token.sh
WORKDIR /data/workspace