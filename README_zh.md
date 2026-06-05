<p align="center">
  <a href="README.md">English</a> | 
  <span style="color:gray;">简体中文</span>
</p>
<h2 align="center">
    <img src="fastlane/metadata/android/en-US/images/icon.png" alt="icon" width="90"/>
    <br />
    <b><a href="https://crustack.github.io/NotallyX/">NotallyX | 极简笔记应用</a></b>
    <p>
        <center>
            <a href="https://ko-fi.com/crustack"><img alt='Donate' height='30' src='documentation/static/img/kofi_donate.svg' /></a>
        </center>
    </p>
    <p>
        <center>
            <a href='https://play.google.com/store/apps/details?id=com.philkes.notallyx&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height='80'/></a>
            <a href="https://f-droid.org/en/packages/com.philkes.notallyx"><img alt='IzzyOnDroid' height='80' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' /></a>
            <a href="https://apt.izzysoft.de/fdroid/index/apk/com.philkes.notallyx"><img alt='F-Droid' height='80' src='https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png' /></a>
        </center>
    </p>
</h2>

<div style="display: flex; justify-content: space-between; width: 100%;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="Image 6" style="width: 32%;"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="Image 2" style="width: 32%;"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="Image 3" style="width: 32%;"/>
</div>

<div style="display: flex; justify-content: space-between; width: 100%;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="Image 4" style="width: 32%;"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="Image 5" style="width: 32%;"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" alt="Image 7" style="width: 32%;"/>
</div>

### 功能特性

[Notally](https://github.com/OmGodse/Notally)，但增强版

<h4><a href="https://crustack.github.io/NotallyX/">查看文档</a></h4>

* 创建**富文本**笔记，支持粗体、斜体、等宽字体和删除线
* 创建**任务清单**并为其添加子任务，支持排序（已勾选的项目自动移至末尾）
* 为重要笔记设置**提醒**并接收通知
* 在笔记中添加任意类型的文件，例如**图片**、PDF 等
* 按标题、最后修改时间、创建时间**排序笔记**
* 使用**颜色、置顶和标签**快速整理笔记
* 在笔记中添加**可点击的链接**，支持电话号码、电子邮件地址和网页 URL
* **撤销/重做**操作
* 使用**主屏幕小部件**快速访问重要笔记
* 通过**生物识别或 PIN 码锁定**笔记
* 可配置的**自动备份**
* 从其他应用**导入笔记**，例如 [Evernote](https://evernote.com/)、[Google Keep](https://keep.google.com/)、[Quillpad](https://quillpad.github.io/)
* 快速创建音频笔记
* 以**列表或网格**形式显示笔记
* 通过文本快速分享笔记
* 丰富的偏好设置，可按喜好调整视图
* 一键清除已完成任务
* 自适应 Android 应用图标
* 支持 Lollipop（Android 5.0）及更高版本设备

---

### 问题反馈 / 功能建议

如果你发现任何 Bug 或希望提出新功能/改进建议，欢迎[创建新 Issue](https://github.com/Crustack/NotallyX/issues/new/choose)

当应用发生未知错误导致崩溃时，你会看到一个对话框（参见 https://github.com/Crustack/NotallyX/pull/171 中的演示视频），你可以在该对话框中直接创建包含崩溃详情的 GitHub 问题报告。

#### 测试版（Beta）发布

在开发过程中，我会不定期发布 BETA 版本。在公开发布新版本之前，获得用户反馈对我非常宝贵。
这些 BETA 版本使用不同的 `applicationId`，因此安装后会在你的设备上显示为一个独立的应用，名为 `NotallyX BETA`。
BETA 版本拥有独立的数据，不会使用你正式版 NotallyX 的数据。
你可以[在此 GitHub 链接](https://github.com/Crustack/NotallyX/releases/tag/beta)下载最新的 BETA 版本。

#### APK 签名证书指纹

如果你想验证下载的 .apk 文件，以下是该应用的证书 SHA256 指纹：
`D2:14:B6:05:7B:79:F8:25:09:DD:CD:1E:35:19:65:B3:C6:EC:C4:B2:A3:89:6E:5C:DF:88:5A:70:A0:B6:1D:FD`

### 翻译

所有翻译均由社区贡献。
有关如何贡献翻译以及支持哪些语言的详细信息，请参阅 [TRANSLATIONS.md](./TRANSLATIONS.md)

### 贡献

如果你想亲自贡献代码，只需任意选择一个无人负责的开放 Issue，留言表示你想要处理它，然后 fork 本仓库并开始开发。

本项目是一个使用 Kotlin 编写的标准 Android 项目，我强烈推荐使用 Android Studio 进行开发。请确保在 `build.gradle` 中定义的 `targetSdk` 所对应的 Android 设备或模拟器上测试你的更改。

在提交 Pull Request 之前，请确保所有测试仍然通过（`./gradlew test`），并运行 `./gradlew ktfmtFormat` 进行统一格式化（该命令也会作为 pre-commit 钩子自动执行）。

### 致谢

原版 Notally 项目由 [OmGodse](https://github.com/OmGodse) 开发，遵循 [GPL 3.0 许可证](https://github.com/OmGodse/Notally/blob/master/LICENSE.md)。

根据 GPL 3.0 的规定，本项目采用相同的 [GPL 3.0 许可证](https://github.com/Crustack/NotallyX/blob/master/LICENSE.md)。
