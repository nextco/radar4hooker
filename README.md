# radar4hooker

`radar4hooker` 是 [`hooker`](https://github.com/CreditTone/hooker) 的增强探测模块。

它运行在目标 Android 应用进程内，通过内置 HTTP 服务把目标 App 的类信息、对象状态、文件、应用信息和当前 UI 暴露出来，方便你在不修改目标 App 代码的情况下做调试、逆向分析和自动化操作。

这个项目不是普通业务 App，而是一个“注入到目标进程里的远程调试面板”。

## 项目重点

它主要解决 4 类问题：

- 运行时类/对象探测
- 内置 HTTP 调试服务
- 当前界面 UI 检查与远程操作
- 反射调用类方法、读取文件、查看应用信息

你可以把它理解成：

- 一个运行在目标 App 里的轻量 HTTP Server
- 一个面向逆向/自动化场景的运行时探针
- 一个给 `hooker` 提供远程观察和控制能力的 `radar.dex`

## 典型用途

例如：

- 查看当前前台 Activity 和页面里的关键控件
- 找出视频播放控件、滚动容器、输入框、按钮、监听器
- 远程点击 `TextView`、`ImageView`、按钮
- 给 `EditText` 写入文字并触发搜索事件
- 反射调用目标类的静态方法或实例方法
- 读取目标 App 的文件、数据库、配置和应用信息

## 主要能力

### 1. UI Inspect

接口：

```text
/hooker/ui/inspect
/hooker/ui/inspect?format=json
```

支持：

- 查看当前前台 Activity
- 收集当前页面重要控件
- 识别视频相关容器和可疑父容器
- 输出控件类名、id、hooker_id、位置、尺寸、文本、监听器等信息
- 导出图片控件预览

HTML 页面内支持直接操作：

- `TextView` 点击
- `ImageView` 点击
- `EditText` 修改文本
- `EditText` 发送搜索事件

### 2. 常用 UI 操作

例如：

- `/hooker/ui/click_by_id`
- `/hooker/ui/set_text`
- `/hooker/ui/send_search_action`
- `/hooker/ui/back`
- `/hooker/ui/home`

### 3. 类辅助能力

例如：

- `/hooker/classhelper/invoke_static_method`
- `/hooker/classhelper/invoke_method`

## 构建

依赖 jar 统一放在 `libs/` 目录。

在 Linux/macOS 下执行：

```bash
sh makeDex4linux.sh
```

会生成两个产物：

- `classes/xradar.jar`
- `classes/radar.dex`

说明：

- `xradar.jar` 可用于你的爬虫注入工程
- `radar.dex` 用于替换 `hooker` 根目录下原有的 `radar.dex`

如果你的 `dx` 在 shell profile 里配置，建议先加载环境再执行：

```bash
source ~/.bash_profile
sh makeDex4linux.sh
```

## 部署

一个常见部署方式是把生成的 `radar.dex` 复制到：

```bash
~/hooker/mobile-deploy/radar.dex
```

## 项目说明

- 项目是老式 `src + jar` 结构，不是标准 Gradle/Maven 工程
- Eclipse 依赖已经调整为从 `libs/` 读取
- `inspect` 页面目前重点增强了短视频场景下的视频容器识别
