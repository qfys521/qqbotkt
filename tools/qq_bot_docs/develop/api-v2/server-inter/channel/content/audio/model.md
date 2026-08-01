# 语音对象 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/audio/model.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/audio/model.html)

---

# 语音对象

## AudioControl

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| audio\_url | string | 音频数据的url status为0时传 |
| text | string | 状态文本（比如：简单爱-周杰伦），可选，status为0时传，其他操作不传 |
| status | STATUS | 播放状态，参考 STATUS |

## STATUS

| 字段名 | 值 | 描述 |
| --- | --- | --- |
| START | 0 | 开始播放操作 |
| PAUSE | 1 | 暂停播放操作 |
| RESUME | 2 | 继续播放操作 |
| STOP | 3 | 停止播放操作 |

## AudioAction

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| guild\_id | string | 频道id |
| channel\_id | string | 子频道id |
| audio\_url | string | 音频数据的url status为0时传 |
| text | string | 状态文本（比如：简单爱-周杰伦），可选，status为0时传，其他操作不传 |