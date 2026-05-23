# LinkBot

一款基于 Android Jetpack Compose 构建的 AI 聊天机器人应用，支持多种 AI 模型和丰富的功能扩展。

## ✨ 功能特性

### 核心功能
- **AI 聊天** - 与 AI 模型进行流畅对话，支持流式响应
- **笔记保存** - 将有价值的 AI 回复保存到笔记，方便后续查看
- **对话历史** - 自动保存对话，支持多会话管理
- **深度思考** - 显示 AI 的推理过程，理解回答背后的逻辑

### 工具调用
- 🔍 **联网搜索** - 获取实时信息
- 🧮 **计算器** - 快速计算
- ⏰ **时间查询** - 获取当前时间
- ☁️ **天气查询** - 查看天气信息

### 个性化设置
- 自定义用户和 AI 头像
- 上下文长度调整
- 推理强度设置
- 主题切换（支持 Monet 配色）

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **组件库**: Miuix KMP
- **玻璃效果**: AndroidLiquidGlass (Backdrop)
- **网络**: OkHttp + Gson
- **图片加载**: Coil
- **数据存储**: AndroidX Datastore

## 📱 界面预览

应用包含四个主要页面：

1. **聊天页面** - 核心对话界面，支持消息发送、编辑、重发
2. **笔记页面** - 管理保存的笔记，支持从笔记继续对话
3. **设置页面** - API 配置、主题设置、功能开关
4. **关于页面** - 应用信息和版本说明

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Gradle 9.4.1
- Android SDK 37

### 构建运行

#### 本地构建
1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接设备或启动模拟器
5. 点击运行按钮

#### CI/CD 构建
本项目使用 GitHub Actions 进行自动化构建。

**⚠️ 重要提示**：使用此仓库的 GitHub Actions 功能前，您需要先在 GitHub 仓库设置中创建以下 Secrets 和 Variables：

**需要创建的 Secrets（Settings → Secrets and variables → Actions）：**
| Secret 名称 | 必填 | 说明 |
|------------|------|------|
| `ANDROID_KEYSTORE_BASE64` | Release 构建时必填 | Base64 编码后的 keystore 文件（release.keystore） |
| `KEYSTORE_PASSWORD` | Release 构建时必填 | keystore 密码 |
| `KEY_ALIAS` | Release 构建时必填 | 密钥别名 |
| `KEY_PASSWORD` | Release 构建时必填 | 密钥密码 |

**创建 Secrets 的方法：**
```bash
# 1. 生成 keystore（如尚未有）
keytool -genkey -v -keystore release.keystore -alias your-key-alias -keyalg RSA -keysize 2048 -validity 10000

# 2. 对 keystore 进行 Base64 编码
base64 -i release.keystore -o keystore_base64.txt

# 3. 将编码后的内容复制到 ANDROID_KEYSTORE_BASE64 secret 中
```

**手动触发构建：**
1. 进入仓库的 **Actions** 页面
2. 选择 **Build** 工作流
3. 点击 **Run workflow**
4. 填写参数：
   - **版本名称**：如 `1.0.0`
   - **签名类型**：选择 `debug`（默认）或 `release`
   - **是否创建 Release**：选择 `false`（默认）或 `true`

**自动构建：**
- 每次推送到 `main` 分支时会自动触发 debug 构建

### API 配置
首次使用需要在设置页面配置 API Key：
1. 切换到「设置」页面
2. 点击「API 配置」
3. 选择 Provider（如 DeepSeek）
4. 输入 API Key
5. 保存配置

## 📁 项目结构

```
app/src/main/java/com/fioiu8/linkbot/
├── MainActivity.kt          # 应用入口
├── data/                    # 数据层
│   ├── AvatarManager.kt     # 头像管理
│   ├── ConversationManager.kt# 对话管理
│   ├── PreferencesManager.kt# 偏好设置
│   └── ToolManager.kt       # 工具调用管理
├── model/                   # 数据模型
│   ├── ChatModels.kt        # 聊天相关模型
│   ├── SavedNote.kt         # 笔记模型
│   └── UserModels.kt        # 用户模型
├── network/                 # 网络层
│   └── ApiService.kt        # API 服务
├── ui/                      # UI 层
│   ├── MainApp.kt           # 应用主组件
│   ├── screens/             # 页面组件
│   │   ├── ChatScreen.kt    # 聊天页面
│   │   ├── NotesScreen.kt   # 笔记页面
│   │   ├── SettingsScreen.kt# 设置页面
│   │   └── AboutScreen.kt   # 关于页面
│   └── theme/               # 主题配置
└── viewmodel/               # ViewModel
    ├── ChatViewModel.kt     # 聊天视图模型
    └── SettingsViewModel.kt # 设置视图模型
```

## 🔧 配置说明

### API 支持的 Provider
- DeepSeek
- 自定义 API（支持自定义 Base URL）

### 支持的模型
- DeepSeek-R1
- DeepSeek-R1-Chat
- 其他兼容 OpenAI API 的模型

## 📄 许可证

MIT License

## 📧 联系方式

如有问题或建议，欢迎提交 Issue 或联系开发者。