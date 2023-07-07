/**
 * @file util
 * @author blueking
 */

import os from 'os'
import path from 'path'

export function resolve(dir) {
  return path.join(__dirname, '..', dir)
}

export function assetsPath(_path) {
  const assetsSubDirectory = 'static'
  return path.posix.join(assetsSubDirectory, _path)
}

export function resolveAssetsPublicPath(url, resourcePath) {
  // TODO: 向上取两级目录， 取与css文件相对地址
  return `../../${url}`
}

export function getIP() {
  const ifaces = os.networkInterfaces()
  const defultAddress = '127.0.0.1'
  let ip = defultAddress

  /* eslint-disable no-loop-func */
  for (const dev in ifaces) {
    if (ifaces.hasOwnProperty(dev)) {
      /* jshint loopfunc: true */
      ifaces[dev].forEach((details) => {
        if (ip === defultAddress && details.family === 'IPv4') {
          ip = details.address
        }
      })
    }
  }
  /* eslint-enable no-loop-func */
  return ip
}
