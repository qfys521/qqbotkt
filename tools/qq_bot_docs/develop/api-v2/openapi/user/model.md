# 用户对象(User) |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/openapi/user/model.html](https://bot.q.qq.com/wiki/develop/api-v2/openapi/user/model.html)

---

# 用户对象(User)

用户对象中所涉及的 ID 类数据，都仅在机器人场景流通，与真实的 ID 无关。请不要理解为真实的 ID

### User

| 字段名 | 类型 | 描述 |
| --- | --- | --- |
| id | string | 用户 id |
| username | string | 用户名 |
| avatar | string | 用户头像地址 |
| bot | bool | 是否是机器人 |
| union\_openid | string | 特殊关联应用的 openid，需要特殊申请并配置后才会返回。如需申请，请联系平台运营人员。 |
| union\_user\_account | string | 机器人关联的互联应用的用户信息，与union\_openid关联的应用是同一个。如需申请，请联系平台运营人员。 |

### 提示

`union_openid` 与 `union_user_account` 只有在单独拉取 member 信息的时候才会提供，在其他的事件中所携带的 user 对象，均无这两个字段的内容。