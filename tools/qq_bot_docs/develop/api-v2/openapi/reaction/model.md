# 表情表态对象 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/openapi/reaction/model.html](https://bot.q.qq.com/wiki/develop/api-v2/openapi/reaction/model.html)

---

# 表情表态对象

### MessageReaction

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| user\_id | string | 用户ID |
| guild\_id | string | 频道ID |
| channel\_id | string | 子频道ID |
| target | ReactionTarget | 表态对象 |
| emoji | [Emoji](/wiki/develop/api-v2/openapi/emoji/model.html#Emoji) | 表态所用表情 |

### ReactionTarget

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| id | string | 表态对象ID |
| type | ReactionTargetType | 表态对象类型，参考 ReactionTargetType |

### ReactionTargetType

| 值 | 描述 |
| --- | --- |
| 0 | 消息 |
| 1 | 帖子 |
| 2 | 评论 |
| 3 | 回复 |