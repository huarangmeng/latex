// 深度压力测试会遍历全部注册命令并验证 20,000 个随机组合。
const testTimeout = 60_000;
config.client.mocha = { ...config.client.mocha, timeout: testTimeout };

// 同步解析期间浏览器无法响应心跳或回传结果，给两条监控链路都留出上报时间。
const browserTimeout = testTimeout + 10_000;
config.pingTimeout = browserTimeout;
config.browserNoActivityTimeout = browserTimeout;
