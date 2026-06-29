# Ji

Ji 是一款面向 Android 12 及以上设备的个人自动记账工具。它在用户已完成支付后，识别微信、支付宝或京东的支付完成页，并调用已配置的云端 VLM 将账单写入本地。

> Ji 只记录已完成的交易，不会发起支付、读取银行卡密码或替用户操作钱包。

## 功能

- 自动识别微信、支付宝、京东的商户支付完成页。
- 支持微信/支付宝转账、微信红包和提现、花呗还款、京东白条还款等专用分类。
- 以分为单位保存金额，自动账单保留平台、交易类型、交易号、发生时间和识别来源。
- 使用单消费者任务队列、稳定页面指纹和持久化去重，避免无障碍高频事件造成重复记账或重复云端调用。
- 支持 Shizuku/Sui 静默截图优先通道；不可用时自动回退到无障碍截图。
- 前台保活、开机恢复与网络恢复重试；可在系统设置中授予忽略电池优化。
- 云端识别服务地址可配置；API Key 输入脱敏，临时识别截图加密保存在应用私有目录并在任务结束后清理。

## 使用要求

- Android 12（API 31）或更高版本。
- 已安装并可正常登录微信、支付宝或京东。
- 在 Ji 的设置页配置你信任的 HTTPS 云端识别服务地址与 API 密钥。
- 手动开启 Ji 的无障碍服务。
- 可选：启动 Shizuku 或 Sui，并在 Ji 中授予截图权限，以提高支付完成页截图稳定性。

## 安装与设置

1. 从 Releases 下载 `app-release.apk` 并安装。
2. 打开 Ji，在“系统设置”中保存云端识别服务地址与 API 密钥。
3. 打开“无障碍服务”。
4. 建议开启“Shizuku 静默截图”和“忽略电池优化”。
5. 完成支付后，Ji 会在后台识别并写入账单；请定期核对自动账单。

## 隐私与安全

- API 密钥存储在 Android 加密存储中，不提交到仓库。
- 自动识别时，支付完成页截图、页面文本、金额、交易号等信息会发送到设置页配置的 HTTPS 云端识别服务。请仅使用你信任并理解其数据处理方式的服务。
- 待识别截图会加密临时保存在应用私有目录；任务成功、忽略或最终失败后会清理。
- 仓库不包含任何 keystore、签名密码、`local.properties`、构建产物或用户账单数据。

## 本地构建

需要 JDK 17、Android SDK 36 和 Android Build Tools。

```powershell
./gradlew.bat testDebugUnitTest lintDebug
./gradlew.bat assembleRelease
```

如需生成签名 Release APK，请通过环境变量提供签名信息，避免将密钥或密码写入仓库：

```powershell
$env:JI_KEYSTORE_PATH = "C:\path\to\keystore.jks"
$env:JI_STORE_PASSWORD = "<store-password>"
$env:JI_KEY_ALIAS = "<key-alias>"
$env:JI_KEY_PASSWORD = "<key-password>"
./gradlew.bat assembleRelease
```

## 文档

- [ARCHITECTURE.md](ARCHITECTURE.md) — 架构概览与包结构
- [TESTING.md](TESTING.md) — 测试指南与 fixture 说明
- [docs/payment-recognition.md](docs/payment-recognition.md) — 支付识别系统详解
- [docs/adr/0001-payment-recognition-pipeline.md](docs/adr/0001-payment-recognition-pipeline.md) — 架构决策记录

## 当前发布版本

`v1.1.0`：支付识别规则引擎重构——将硬编码关键词列表提取为 JSON 数据驱动的 `PaymentCompletionRuleEngine`，每次分类返回可追踪的 `PaymentRuleTrace`（ruleId、decision、matchedKeywords）。新增 23 个 fixture 测试覆盖微信/支付宝/京东全部 accept/reject 场景。Room schema v5、统计图表组件、Home/ExtraBill/Settings 模块重构拆分、新增 instrumented tests。

`v1.0.7`：吸收安全审查反馈，支持配置 HTTPS 云端识别地址、API Key 脱敏输入、临时截图 AES-GCM 加密落盘、自动识别失败通知；金额转分改为 BigDecimal，修复通知 ID hash 碰撞和自动账单冲突时 transactionId=0 的风险。

`v1.0.6`：强化支付宝/微信账单详情与支付消息页拦截，避免查看历史账单时重复记账；缺少交易号时增加 15 分钟复合兜底去重。新增无障碍看门狗保活、开机后 Shizuku 掉线提醒、安全降采样截图解码，并缩短云端 VLM 网络超时。

`v1.0.5`：支付候选改用“平台 + 交易类型 + 交易号”语义去重；缺少交易号时仅在同平台、同金额的 20 秒内合并同页回调。修复微信仅显示“支付成功”时漏记，并补齐弹窗与底部页面的逐级返回。

`v1.0.4`：交易/订单号参与页面和账单去重；同一商户的连续两笔独立付款不会因金额或时间接近被错误合并。
