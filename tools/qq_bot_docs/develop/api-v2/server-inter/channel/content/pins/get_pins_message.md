# 获取精华消息 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/pins/get_pins_message.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/pins/get_pins_message.html)

---

# 获取精华消息

## 接口

```
GET /channels/{channel_id}/pins
```

1

## 功能描述

用于获取子频道 `channel_id` 内的精华消息。

## Content-Type

```
application/json
```

1

## 返回

返回 [PinsMessage](/wiki/develop/api-v2/server-inter/channel/content/pins/model.html#PinsMessage) 对象。

## 错误码

详见[错误码](/wiki/develop/api-v2/openapi/error/error.html)。

## 示例

响应数据包

```
{
  "guild_id": "xxxxxx",
  "channel_id": "xxxxxx",
  "message_ids": ["xxxxx"]
}
```

1  
2  
3  
4  
5