export function openUrlWithInputTimeoutHack(url, failCb, successCb) {
  window.changeAlert = false;
  const target = document.createElement('input');
  target.style.width = '0';
  target.style.height = '0';
  target.style.position = 'fixed';
  target.style.top = '0';
  target.style.left = '0';
  document.body.appendChild(target);

  target.focus();
  const handler = _registerEvent(target, 'blur', onBlur);
  function onBlur() {
    successCb && successCb();
    handler.remove();
    clearTimeout(timeout);
    document.body.removeChild(target);
  }

  // will trigger onblur
  location.href = url;

  // Note: timeout could vary as per the browser version, have a higher value
  const timeout = setTimeout(function () {
    console.log('setTimeout');
    failCb && failCb();
    handler.remove();
    document.body.removeChild(target);
  }, 1000)
}

function _registerEvent(target, eventType, cb) {
  if (target.addEventListener) {
    target.addEventListener(eventType, cb);
    return {
      remove: function () {
        target.removeEventListener(eventType, cb);
      }
    }
  } else {
    target.attachEvent(eventType, cb);
    return {
      remove: function () {
        target.detachEvent(eventType, cb);
      }
    }
  }
}
