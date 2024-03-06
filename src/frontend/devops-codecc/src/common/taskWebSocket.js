import SockJS from 'sockjs-client';
const { Stomp } = require('stompjs/lib/stomp.js');

class TaskWebSocket {
  constructor() {
    this.errTime = 1;
    this.taskId = 0;
    this.stompClient = {};
  }

  connect(...args) {
    this.disconnect().then(this.build(...args));
  }

  disconnect() {
    if (this.stompClient.send && this.taskId) {
      this.stompClient.send(
        '/app/cleanTaskSession',
        {},
        JSON.stringify({ taskId: this.taskId }),
      );
    }
    return new Promise((resolve) => {
      if (this.stompClient.connected) this.stompClient.disconnect(resolve);
      else resolve();
    });
  }

  subscribeMsg(subscribeUrl, { success }) {
    this.stompClient.subscribe(subscribeUrl, (res) => {
      success(res);
    });
  }

  build(projectId, taskId, subscribeUrl, { success, error }) {
    this.taskId = taskId;
    const socket = new SockJS(`${window.AJAX_URL_PREFIX}/codeccjob/websocket/user/taskLog/analysisInfo
?X-DEVOPS-PROJECT-ID=${projectId}&X-DEVOPS-TASK-ID=${taskId}`);
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;
    this.stompClient.connect(
      {},
      () => {
        this.subscribeMsg(subscribeUrl, { success });
      },
      (err) => {
        if (this.errTime <= 8) {
          // 由于部署原因，可能会出现需要重连的情况
          this.errTime += 1;
          setTimeout(
            () => this.connect(projectId, taskId, subscribeUrl, { success, error }),
            10000,
          );
        } else {
          this.disconnect();
          error(err.message || 'websocket异常');
        }
      },
    );
  }
}

export default new TaskWebSocket();
