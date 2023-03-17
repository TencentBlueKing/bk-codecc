/**
 * @file 引入 bk-magic-vue 组件
 * @author blueking
 */
/* eslint-disable */
import Vue from 'vue'
import '@tencent/bk-magic-vue/dist/bk-magic-vue.min.css'
import bkMagic from '@tencent/bk-magic-vue'

Vue.use(bkMagic)

const Message = Vue.prototype.$bkMessage

let messageInstance = null

export const messageError = (message, delay = 3000) => {
  messageInstance && messageInstance.close()
  messageInstance = Message({
    message,
    delay,
    ellipsisLine: 10,
    theme: 'error'
  })
}

export const messageSuccess = (message, delay = 3000) => {
  messageInstance && messageInstance.close()
  messageInstance = Message({
    message,
    delay,
    theme: 'success'
  })
}

export const messageInfo = (message, delay = 3000) => {
  messageInstance && messageInstance.close()
  messageInstance = Message({
    message,
    delay,
    theme: 'primary'
  })
}

export const messageWarn = (message, delay = 3000) => {
  messageInstance && messageInstance.close()
  messageInstance = Message({
    message,
    delay,
    theme: 'warning',
    hasCloseIcon: true
  })
}

Vue.prototype.messageError = messageError
Vue.prototype.messageSuccess = messageSuccess
Vue.prototype.messageInfo = messageInfo
Vue.prototype.messageWarn = messageWarn
