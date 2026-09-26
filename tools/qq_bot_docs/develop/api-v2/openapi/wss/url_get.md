# 获取通用 WSS 接入点 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/openapi/wss/url_get.html](https://bot.q.qq.com/wiki/develop/api-v2/openapi/wss/url_get.html)

---

# 获取通用 WSS 接入点

### 接口

```
GET /gateway
```

1

### 功能描述

用于获取 WSS 接入地址，通过该地址可建立 `websocket` 长连接。

### Content-Type

```
application/json
```

1

### 返回

返回一个用于连接 `websocket` 的地址。

### 错误码

详见[错误码](/wiki/develop/api-v2/openapi/error/error.html)。

### 示例

响应数据包

```
{
  "url": "wss://api.bot.qq.com/websocket/"
}
```

1
2
3