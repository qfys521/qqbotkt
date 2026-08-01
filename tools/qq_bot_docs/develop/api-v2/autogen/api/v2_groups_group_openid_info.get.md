# 获取群基础信息 |  QQ 机器人官方文档

> 原始链接: [https://bot.q.qq.com/wiki/develop/api-v2/autogen/api/v2_groups_group_openid_info.get.html](https://bot.q.qq.com/wiki/develop/api-v2/autogen/api/v2_groups_group_openid_info.get.html)

---

# 获取群基础信息

获取指定群的基本信息。

该接口能力正在内邀开放中

## 请求

### 基础信息

| 字段 | 值 |
| --- | --- |
| HTTP URL | /v2/groups/{group\_openid}/info |
| HTTP Method | GET |
| 接口频率限制 | 60 QPM |

## 路径参数

| 名称 | 类型 | 必填 | 描述 |
| --- | --- | --- | --- |
| group\_openid | string | 是 | 群 OpenID |

### 请求示例

**获取群信息**

```
GET /v2/groups/3E5D8A1F7B2C9E4D6A0F1B3C5D7E9F2A/info
```

1

## 响应

### 响应体

| 名称 | 类型 | 描述 |
| --- | --- | --- |
| group\_openid | string | 群 OpenID |
| group\_name | string | 群名称 |
| group\_finger\_memo | string | 群简介 |
| group\_class\_text | string | 群分类 |
| group\_tags | []string | 群标签列表 |
| group\_member\_num | integer | 群成员人数 |

## 响应示例

**获取群信息**

```
{
  "group_openid": "3E5D8A1F7B2C9E4D6A0F1B3C5D7E9F2A",
  "group_name": "读书分享会",
  "group_finger_memo": "每周共读一本好书",
  "group_class_text": "文化",
  "group_tags": [
    "阅读",
    "文学",
    "成长"
  ],
  "group_member_num": 256
}
```

1  
2  
3  
4  
5  
6  
7  
8  
9  
10  
11  
12

### 错误码

| 错误码 | 描述 | 排查建议 |
| --- | --- | --- |
| 11253 | 应用无接口访问权限 | 该接口仅白名单机器人可用，请联系平台运营申请权限 |