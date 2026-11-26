# 岛屿机 · 长期说明书

@docs/开工必读.md

> 上面这一行的意思:每次开工,先把「开工必读」整份读进来,再按里面的指引读其他文档。没读完不许动手。

---

## 我是谁

我是"岛屿机"项目的开发搭档。user 说的都是大白话,不懂术语,我回复时用通俗直白的话讲。

## 项目一句话

纯原生安卓 APK,在手机里模拟一台带 16 个 App 的"小手机",主玩法是和 AI 角色(char)过日子。详见 `README.md`。

## 技术栈(已定,不用再问 user)

| 项目 | 选择 | 大白话 |
|---|---|---|
| 语言 | Kotlin | 安卓官方主力语言 |
| 界面 | Jetpack Compose,自己画苹果风组件 | 不用安卓默认的"安卓味"控件 |
| 数据库 | Room(底层 SQLite,开 WAL) | 系统自带数据库,微信同类方案 |
| 异步 | Kotlin 协程 + Flow | 让界面不卡 |
| 网络 | OkHttp,流式输出自己写解析 | 一个字一个字蹦出来,自己控制 |
| 保活 | 前台服务 + WorkManager | 切后台也能等到回复 |
| 依赖注入 | Koin | 各模块拼装的胶水 |
| 序列化 | kotlinx.serialization | 存取数据格式 |
| 图片 | Coil | 加载图片 |
| 音乐 | Media3 | 播放器 |
| 密钥保存 | Android Keystore 加密 | API 密钥加密存放 |
| 向量记忆 | 向量存在数据库里,查询时分批比对 | 后续可换更快的原生库 |
| 后端 | Cloudflare Workers + D1 | 免费额度,仅用于登录/邀请码/更新 |

- 包名:`com.island.phone`(一旦发布不能改)
- 最低系统:Android 8.0(API 26);目标:API 35
- 签名:读环境变量 `KEYSTORE_PATH`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`,密钥库文件永远不进仓库
- 版本号:根目录 `version.properties`(`VERSION_NAME`、`VERSION_CODE`),每次更新必须升

## 目录结构

```
app/src/main/java/com/island/phone/
├─ core/            全局共用:主题、页面跳转、弹窗、通用组件、工具
├─ data/            数据库、数据存取(只管存取,不管界面)
├─ ai/              调用模型、流式、共用读取流程、记忆、驱动系统
├─ service/         保活、通知、下载更新
├─ home/            加载动画、锁屏、主屏幕、桌面组件
└─ apps/
   ├─ chat/ appearance/ years/ album/ calendar/ settings/ worldbook/ music/
   ├─ meet/ patrol/ forum/ novel/ couple/ preset/ meituan/ dream/
   └─ 每个 App 里再分:ui(界面) / vm(界面状态) / repo(存取) / model(模型调用相关)
docs/               所有规则与计划
docs/rules/         按主题拆开的规则
docs/design/        苹果官方设计依据(下一轮整理)
.github/workflows/  自动打包
```

## 代码规则(摘要,细则在 docs/rules/代码结构.md)

1. 一个文件只管一类事:界面、存取数据、调用模型各自独立。
2. 文件太长或混了多个功能,立刻拆。多个 App 共用的东西统一放 `core/` 或 `ai/`。
3. 新功能沿用已有结构,不另起炉灶。
4. 整理代码后,必须检查原有功能是否正常。
5. 更新优化时,不能随意删除没涉及到的代码;涉及到的才可以改或删。
6. user 能创建的内容,必须同时有编辑和删除;能开启的功能,必须同时有关闭方式。
7. 页面、弹窗、功能不加小字注释和说明文字。
8. 每个新页面/弹窗/功能,都要适配日间/夜间/跟随系统/手动切换,并支持左右滑返回。
9. 所有弹窗用专属弹窗样式。

## 每次回复必须交代

- 改了什么(大白话)
- 改了哪些文件
- 一共改了几个文件
- 新版本号(每次更新默认升版本)

## 经验规则

反复出现的问题,解决办法会总结成短规则,补进 `docs/开工必读.md` 的「经验规则」。

## 怎么保证每次开工自动读到

- Claude Code:仓库根目录的 `CLAUDE.md` 会被自动加载,上面 `@docs/开工必读.md` 会把总入口一起带进来。
- 在聊天窗口里:先说"开工",我会先去仓库读 `CLAUDE.md` 和 `docs/开工必读.md`,再动手。
