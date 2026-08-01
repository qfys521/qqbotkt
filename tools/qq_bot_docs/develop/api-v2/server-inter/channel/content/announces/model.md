# 公告对象(Announces) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/announces/model.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/content/announces/model.html)

---

# 公告对象(Announces)

## Announces

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| guild\_id | string | 频道 id |
| channel\_id | string | 子频道 id |
| message\_id | string | 消息 id |
| announces\_type | uint32 | 公告类别 0:成员公告 1:欢迎公告，默认成员公告 |
| recommend\_channels | [RecommendChannel](#RecommendChannel) 数组 | 推荐子频道详情列表 |

# 推荐子频道对象(RecommendChannel)

## RecommendChannel

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| channel\_id | string | 子频道 id |
| introduce | string | 推荐语 |