import SockJS from 'sockjs-client';
import { Stomp } from 'stompjs/lib/stomp.js';

class ProjectWebSocket {
  constructor() {
    this.errTime = 1;
    this.stompClient = {};
  }

  connect(...args) {
    this.disconnect().then(this.build(...args));
  }

  disconnect() {
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

  build(projectId, subscribeUrl, { success, error }) {
    const socket = new SockJS(`${window.AJAX_URL_PREFIX}/codeccjob/websocket/user
/taskLog/analysisInfo?X-DEVOPS-PROJECT-ID=${projectId}`);
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
            () => this.connect(projectId, subscribeUrl, { success, error }),
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

export default new ProjectWebSocket();
