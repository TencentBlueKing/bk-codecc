import SockJS from 'sockjs-client';
import { Stomp } from 'stompjs/lib/stomp.js';

class DefectWebSocket {
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

  build(getSuggestion, subscribeUrl, { success, error }) {
    const socket = new SockJS(`${window.AJAX_URL_PREFIX}/defect/websocket/user/warn`);
    this.stompClient = Stomp.over(socket);
    this.stompClient.debug = null;
    this.stompClient.connect(
      {},
      () => {
        this.subscribeMsg(subscribeUrl, { success });
        getSuggestion();
      },
      (err) => {
        console.log('🚀 ~ file: defectWebSocket.js:52 ~ DefectWebSocket ~ build ~ err:', err);
        if (this.errTime <= 8) {
          // 由于部署原因，可能会出现需要重连的情况
          this.errTime += 1;
          setTimeout(
            () => this.connect(getSuggestion, subscribeUrl, { success, error }),
            10000,
          );
        } else {
          this.disconnect();
          error(err.message || 'websocket异常');
        }
      },
    );
  }

  sendMessage(params) {
    const { stompClient } = this;
    if (stompClient) {
      const { uid, projectId, taskId, ...rest } = params;
      const message = {
        body: rest,
        headers: {
          'X-DEVOPS-UID': uid,
          'X-DEVOPS-PROJECT-ID': projectId,
          'X-DEVOPS-TASK-ID': taskId,
        },
      };
      stompClient.send(
        '/app/defect/suggestion', message.headers,
        JSON.stringify(message.body),
      );
    }
  }
}

export default DefectWebSocket;
