/**
 * @file event bus
 * @author blueking
 */

import Vue from 'vue';

// Use a bus for components communication,
// see https://vuejs.org/v2/guide/components.html#Non-Parent-Child-Communication
// eslint-disable-next-line
export const bus = new Vue();
