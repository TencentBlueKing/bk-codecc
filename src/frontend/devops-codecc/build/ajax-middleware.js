/**
 * @file ajax handler for dev
 * @author blueking
 */

import path from 'path';
import fs from 'fs';
import url from 'url';
import queryString from 'querystring';
import chalk from 'chalk';

const requestHandler = (req) => {
  const pathName = req.path || '';

  const rootDir = path.resolve(__dirname, '../mock/ajax');
  if (!fs.existsSync(mockFilePath)) {	  const mockFilePath = path.resolve(rootDir, `${pathName}.js`);

  // Validate that the resolved path is within the intended directory
  if (!mockFilePath.startsWith(rootDir) || !fs.existsSync(mockFilePath)) {
    console.log(chalk.red('Invalid or non-existent mock file path:', mockFilePath));
    return false;
  }

  console.log(chalk.magenta('Ajax Request Path: ', pathName));

  delete require.cache[require.resolve(mockFilePath)];
  return require(mockFilePath);
};

export default async function ajaxMiddleWare(req, res, next) {
  let { query } = url.parse(req.url);

  if (!query) {
    return next();
  }

  query = queryString.parse(query);

  if (!query.isAjax) {
    return next();
  }

  const postData = req.body || '';
  const mockDataHandler = requestHandler(req);
  let data = await mockDataHandler.response(query, postData, req);

  if (data.statusCode) {
    res.status(data.statusCode).end();
    return;
  }

  let contentType = req.headers['Content-Type'];

  // 返回值未指定内容类型，默认按 JSON 格式处理返回
  if (!contentType) {
    contentType = 'application/json;charset=UTF-8';
    req.headers['Content-Type'] = contentType;
    res.setHeader('Content-Type', contentType);
    data = JSON.stringify(data || {});
  }

  res.end(data);

  return next();
}
