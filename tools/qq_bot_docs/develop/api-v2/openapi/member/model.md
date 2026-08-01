# 成员对象(Member) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/openapi/member/model.html](https://bot.q.qq.com/wiki/develop/api-v2/openapi/member/model.html)

---

# 成员对象(Member)

### Member

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| user | [User](/wiki/develop/api-v2/openapi/user/model.html#user) | 用户的频道基础信息，只有成员相关接口中会填充此信息 |
| nick | string | 用户的昵称 |
| roles | string 数组 | 用户在频道内的身份组ID, 默认值可参考[DefaultRoles](/wiki/develop/api-v2/openapi/guild/role_model.html#DefaultRoles) |
| joined\_at | ISO8601 timestamp | 用户加入频道的时间 |

### MemberWithGuildID

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| guild\_id | string | 频道id |
| user | [User](/wiki/develop/api-v2/openapi/user/model.html#user) | 用户的频道基础信息 |
| nick | string | 用户的昵称 |
| roles | string 数组 | 用户在频道内的身份 |
| joined\_at | ISO8601 timestamp | 用户加入频道的时间 |