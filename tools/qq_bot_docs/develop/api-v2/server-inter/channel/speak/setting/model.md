# 频道消息频率设置对象 (MessageSetting) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/speak/setting/model.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/speak/setting/model.html)

---

# 频道消息频率设置对象 (MessageSetting)

## MessageSetting

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| disable\_create\_dm | string | 是否允许创建私信 |
| disable\_push\_msg | string | 是否允许发主动消息 |
| channel\_ids | string 数组 | 子频道 id 数组 |
| channel\_push\_max\_num | uint32 | 每个子频道允许主动推送消息最大消息条数 |