# 频道对象(Guild) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/manage/guild/model.html](https://bot.q.qq.com/wiki/develop/api-v2/server-inter/channel/manage/guild/model.html)

---

# 频道对象(Guild)

频道对象中所涉及的 ID 类数据，都仅在机器人场景流通，与真实的 ID 无关。请不要理解为真实的 ID

## Guild

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| id | string | 频道ID |
| name | string | 频道名称 |
| icon | string | 频道头像地址 |
| owner\_id | string | 创建人用户ID |
| owner | bool | 当前人是否是创建人 |
| member\_count | int | 成员数 |
| max\_members | int | 最大成员数 |
| description | string | 描述 |
| joined\_at | string | 加入时间 |