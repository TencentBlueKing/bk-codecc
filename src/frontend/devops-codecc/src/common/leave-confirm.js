import Vue from 'vue';
import I18n from '@/i18n';

// eslint-disable-next-line import/prefer-default-export
export const leaveConfirm = (message = I18n.t('离开将会导致未保存信息丢失')) => {
  if (!window.changeAlert) {
    return Promise.resolve(true);
  }
  const vm = new Vue();
  const h = vm.$createElement;
  return new Promise((resolve, reject) => {
    vm.$bkInfo({
      title: I18n.t('确认离开当前页？'),
      subHeader: h(
        'p',
        {
          style: {
            color: '#63656e',
            fontSize: '14px',
            textAlign: 'center',
          },
        },
        message,
      ),
      okText: I18n.t('离开'),
      confirmFn: () => {
        window.changeAlert = false;
        resolve(true);
      },
      cancelFn: () => {
        reject(Error('cancel'));
      },
    });
  });
};
