// 深度压力测试会遍历全部注册命令并验证 20,000 个随机组合。
config.client.mocha = { ...config.client.mocha, timeout: 60_000 };
