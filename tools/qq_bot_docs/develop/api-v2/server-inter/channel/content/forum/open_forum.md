# 开放论坛事件对象(OpenForumEvent) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/forum/open_forum.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/forum/open_forum.html)

---

# 开放论坛事件对象(OpenForumEvent)

## OEPN\_FORUM\_EVENT（intents OPEN\_FORUM\_EVENT）

**发送时机**

* 用户在话题子频道内发帖、评论、回复评论时产生该事件

## 主题事件

* OPEN\_FORUM\_THREAD\_CREATE
* OPEN\_FORUM\_THREAD\_UPDATE
* OPEN\_FORUM\_THREAD\_DELETE

### 示例

```
{
  "guild_id": "47129941624960822",
  "channel_id": "1661124",
  "author_id": "144115218182563108",
}
```

1  
2  
3  
4  
5

## 帖子事件

* OPEN\_FORUM\_POST\_CREATE
* OPEN\_FORUM\_POST\_DELETE

### 示例

```
{
  "guild_id": "47129941624960822",
  "channel_id": "1661124",
  "author_id": "144115218182563108",
}
```

1  
2  
3  
4  
5

## 回复事件

* OPEN\_FORUM\_REPLY\_CREATE
* OPEN\_FORUM\_REPLY\_DELETE

### 示例

```
{
  "guild_id": "47129941624960822",
  "channel_id": "1661124",
  "author_id": "144115218182563108",
}
```

1  
2  
3  
4  
5