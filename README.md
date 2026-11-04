# GD音乐 (GDMusic)

基于 [GD音乐台 API](https://music-api.gdstudio.xyz/api.php) 的原生安卓在线音乐播放器。

- 技术栈：Kotlin + Jetpack Compose (Material 3) + Media3/ExoPlayer
- 系统要求：Android 8.0+（小米13 / HyperOS 3.0 完全兼容）
- 预计体积：约 4~6 MB
- 出处要求：使用 GD音乐台(music.gdstudio.xyz) API，**仅限学习使用，严禁商用**

## 功能

- 🔍 搜索歌曲 / 歌手 / 专辑（支持网易云、酷我、JOOX、B站四个音源）
- 🎵 在线播放，后台播放 + 通知栏 / 锁屏控制（Media3 MediaSession）
- 🖼️ 专辑封面展示（播放页大图 + 列表缩略图）
- 📝 LRC 歌词同步滚动高亮 + 中文翻译
- 🔊 音质切换：128K / 192K / 320K / 无损（以 API 实际返回为准）
- 🕘 最近搜索记录
- 🎨 Material You 动态取色（跟随小米13 系统壁纸），支持主题图标

## 如何获得 APK（无需安装任何开发环境）

本仓库已配置 GitHub Actions 云构建，步骤如下：

1. **创建仓库**：登录 [github.com](https://github.com) → 右上角 `+` → `New repository`
   - 名称填 `GDMusic`，选 `Public`（Private 也可以，Public 的 Actions 免费额度更多）→ `Create repository`

2. **上传源码**：仓库页面中间点击 **`uploading an existing file`** 链接
   - 把本 `GDMusic` 文件夹里的**全部内容**（包括 `.github` 文件夹！）拖进浏览器上传区
   - ⚠️ `.github` 是隐藏文件夹：Windows 文件资源管理器需先在「查看」中勾选「隐藏的项目」
   - 上传清单检查：`.github/workflows/build.yml`、`app/`、`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`.gitignore`、本 README
   - 点击 `Commit changes`

3. **等待云构建**：进入仓库的 `Actions` 标签页，会看到 `Build APK` 正在运行（约 3~8 分钟）

4. **下载 APK**：构建完成后点进该次运行 → 页面底部 `Artifacts` 区域 → 下载 `GDMusic-APK` → 解压得到 `GDMusic-v1.0.apk`

5. **安装到小米13**：
   - 把 APK 传到手机（微信文件传输助手 / USB / 小米互传均可）
   - 点击安装；若 HyperOS 提示安全风险，选择「继续安装」或在 设置 → 更多设置 → 系统安全 中临时关闭「纯净模式」

## 后续改版

修改代码后重新上传改动的文件（或用 git push），Actions 会自动重新构建新版 APK。

## 目录结构

```
GDMusic/
├── .github/workflows/build.yml      # 云构建工作流
├── app/
│   ├── build.gradle.kts             # 应用模块配置（依赖、压缩）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/xucheng/gdmusic/
│       │   ├── MainActivity.kt      # 入口 + 全局状态 + 播放控制逻辑
│       │   ├── SearchScreen.kt      # 搜索页 + 迷你播放条
│       │   ├── PlayerScreen.kt      # 播放页 + LRC 歌词
│       │   ├── PlayerService.kt     # 后台播放服务（通知栏控制）
│       │   ├── GdApi.kt             # GD 音乐台 API 客户端
│       │   ├── Icons.kt             # 手绘图标（减小体积）
│       │   └── Theme.kt             # Material You 动态主题
│       └── res/                     # 图标、文案
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 注意事项

- API 有频率限制：**5 分钟内不超过 50 次请求**，刷太快会提示 503，稍等即可
- 播放链接有时效性，App 每次播放都实时获取，不存在过期问题
- 「无损」音质以 API 实际返回为准，请求 999 不一定拿到 24bit
