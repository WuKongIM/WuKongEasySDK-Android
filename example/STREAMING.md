# Android 流式聊天 Demo

在 `example` 应用右上角选择 **流式聊天 Demo**。这是原生 Android 页面，使用本仓库
SDK 的两个独立实例，分别模拟用户和助手；对齐 WuKongIM 的
[`demo/streamdemo`](https://github.com/WuKongIM/WuKongIM/tree/main/demo/streamdemo)。

## 运行

环境：JDK 17、Android SDK 34、Android 5.0 / API 21 及以上设备。

```sh
./gradlew :example:assembleDebug
./gradlew :example:installDebug
```

1. 启动当前 WuKongIM v3 服务端，启用 `wsmux` WebSocket 监听器。
2. 输入 Android 可访问的 HTTP API 地址。模拟器访问宿主默认使用
   `http://10.0.2.2:5001`；真机使用开发机的局域网地址。
3. `/route` 的 `ws_addr` / `wss_addr` 也必须是设备可访问的地址。
   例如模拟器可用 `ws://10.0.2.2:5200`，真机用开发机的局域网地址。
   若用 `adb reverse`，需同时映射 API 与 WebSocket 端口，路由使用
   `ws://127.0.0.1:<映射的 WebSocket 端口>`。
4. 点击“创建会话并连接”。Demo 随机创建两个账号和一个群频道；两个连接完成且
   历史恢复成功后显示“在线”，此时可发送问题。
5. 打开“演示设置”，调整模拟文本、每段字数、间隔，或单独生成回复。
   生成时可“取消生成”或“模拟失败”。断开接收端后仍可单独生成，重连后恢复完整快照。

账号与 Token 仅保存在当前屏幕的 ViewModel 内存中，旋转屏幕不会创建新身份或
重复生成。退出页面、进程终止后会话凭据不再保留；再次进入会创建新会话。
“新会话”只释放当前连接与内存视图，不删除服务器历史，也不修改旧账号。
突然退出或进程终止会中止生成，未确认的终态需要由实际业务后端处理。

本 Demo 直接调用 `/user/token`、`/channel`、`/message/event` 和历史 API，适用于
可写演示数据的开发环境。生产应用应由业务后端验证身份、作者和频道权限，并托管
模型凭据及流事件写入。

## 真实模型

在“演示设置”中勾选“使用兼容模型的 SSE 回复”，填写：

- 模型 URL：兼容 Chat Completions 的基础地址（例如 `https://provider.example/v1`），
  或完整的 `/chat/completions` 地址；查询参数会保留。
- 模型名称：填写服务商提供的模型 ID，本示例不自动请求模型目录。
- API Key：不要求鉴权的本机模型可以留空。

发送问题后，Android 直接请求模型的 SSE 接口。无需浏览器 CORS 代理。
请求携带最近已完成的问答；取消与失败的助手回复不作为完成答案加入后续上下文。
Key 不进入消息、日志、Bundle、磁盘或报告，重建 Activity 后需要重新填写。
HTTP 错误正文不会显示。取消会关闭上游请求；意外 EOF 或 SSE 错误保存已确认的部分
内容并写入失败终态。仅支持文本 Chat Completions，不处理工具调用或多模态内容。

请求最长 180 秒，读取空闲超时 30 秒，不跟随重定向。请求体最多 128 KiB，
SSE 流最多 4 MiB、单行与事件最多 64 KiB、回复最多 16,384 字符。
上下文最多 39 条、24,000 字符；聊天视图最多 40 条、日志 100 行。

## SDK 能力

流消息能力随 SDK `1.1.0` 提供；`example` 通过 `project(':')` 使用本地 SDK。已有 `getInstance()` 和 `send(channel, type, payload, header, topic)` 调用保留。

```kotlin
val peer = WuKongEasySDK.createInstance()
peer.init(context, config)
// listener is a strong application-owned reference; the SDK stores weak references.
val listener = object : WuKongEventListener<EventNotification> {
    override fun onEvent(data: EventNotification) {
        // data.type == "stream.delta"; data.data contains channel, key, payload and UTF-8 offset.
    }
}
peer.addEventListener(WuKongEvent.CUSTOM_EVENT, listener)
peer.connect()
val key = UUID.randomUUID().toString()
val ack = peer.send(channel, WuKongChannelType.GROUP, mapOf("type" to 1, "content" to ""),
    SendOptions(clientMsgNo = key, setting = MessageSetting(stream = true)))
check(ack.reasonCode == 1)
// Only now may the backend append /message/event for this key.
peer.destroy() // On screen/session disposal; keep connected while using the stream.
```

`WuKongEvent.CUSTOM_EVENT` 接收 JSON-RPC `event` 通知，结构为
`{id, type, timestamp, data}`，兼容对象或 JSON 字符串形式的 `data`。
不会对它发送 RECVACK，也不提供离线事件队列。

Demo 的生产顺序为：成功 SENDACK → `stream.open` → `stream.delta` × N →
`stream.finish`。取消／失败先写 `stream.cancel`／`stream.error`，再以已确认文本
写 `stream.finish`。每个事件拥有稳定 ID；写入结果未确认时停止追加，提示用户重连
查看历史，避免用新事件 ID 重试导致重复。

接收端按 Event ID 去重，按 UTF-8 字节 `text_offset` 合并重复和重叠增量。
仅在每次成功连接／重连时请求一次 `/channel/messagesync`，使用
`event_summary_mode: "full"`；恢复期间缓冲实时事件，先应用快照再回放增量。
在线时不轮询历史、不调用 `/message/eventsync`。

## 可重复验收

```sh
# Android SDK 根目录
./gradlew testDebugUnitTest :example:testDebugUnitTest \
  :example:assembleDebug :example:assembleDebugAndroidTest

# 另在 WuKongIM 服务端仓库构建
GOWORK=off go build -o /tmp/wukongim-android-stream-demo ./cmd/wukongim

# 返回 Android SDK 根目录；启动一个 API 34 模拟器或连接设备
python3 example/tools/stream_acceptance.py \
  --server-bin /tmp/wukongim-android-stream-demo \
  --serial emulator-5554 \
  --output /tmp/wk-android-stream-acceptance
```

脚本启动临时单节点集群（256 hash slots）与本机 SSE 模型夹具，映射所需端口，
安装本地 APK，驱动原生 UI。它验证逐段回复、中文／emoji、取消／失败、离线生成、
重连恢复、Activity 重建、模型 SSE、多轮上下文、模型错误与断流、上游取消和无在线
历史轮询。退出时回收自建服务及本次映射，保留 `report.json`、PNG 截图、测试输出与
服务端日志。无需真实模型 Key 或付费请求。

ATD 模拟器默认禁用绘制；若要生成有效截图，运行前执行：

```sh
adb -s emulator-5554 shell setprop debug.hwui.drawing_enabled 1
```

`docs/STREAMING_DEMO_VALIDATION.md` 记录实现前列出的失败场景。
