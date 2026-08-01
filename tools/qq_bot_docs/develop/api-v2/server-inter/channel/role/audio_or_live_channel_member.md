# 音视频/直播子频道成员进出事件 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/role/audio_or_live_channel_member.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/role/audio_or_live_channel_member.html)

---

# 音视频/直播子频道成员进出事件

## AUDIO\_OR\_LIVE\_CHANNEL\_MEMBER\_ENTER

### 发送时机

* 用户进入音视频/直播子频道时

### 示例

```
{
  "guild_id": "47129941624960822",
  "channel_id": "1661124",
  "channel_type": 2, // 2-音视频子频道 5-直播子频道
  "user_id": "144115218182563108"
}
```

1  
2  
3  
4  
5  
6

## AUDIO\_OR\_LIVE\_CHANNEL\_MEMBER\_EXIT

### 发送时机

* 用户离开音视频/直播子频道时

### 示例

```
{
  "guild_id": "47129941624960822",
  "channel_id": "1661124",
  "channel_type": 2, // 2-音视频子频道 5-直播子频道
  "user_id": "144115218182563108"
}
```

1  
2  
3  
4  
5  
6