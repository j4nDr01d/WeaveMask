# 构建和开发

## 设置环境

- 支持的平台：
  - Linux x64
  - macOS x64 (Intel)
  - macOS arm64 (Apple Silicon)
  - Windows x64
- 仅限 Windows：启用[开发者模式](https://learn.microsoft.com/en-us/windows/apps/get-started/enable-your-device-for-development)。这是必需的，因为我们需要符号链接支持。
- 安装 Python 3.8+：
  - 在 Unix 上，使用你喜欢的包管理器安装 python3
  - 在 Windows 上，在[官方网站](https://www.python.org/downloads/windows/)下载并安装最新的 Python 版本。<br>
    确保在安装过程中选择**"Add Python to PATH"**。
  - （Windows 可选）：运行 `pip install colorama` 安装 `colorama` python 包
- 安装 Git：
  - 在 Unix 上，使用你喜欢的包管理器安装 git
  - 在 Windows 上，在[官方网站](https://git-scm.com/download/win)下载并安装最新的 Git 版本。<br>
    确保在安装过程中**"启用符号链接"**。
- 安装 Android Studio 并按照说明完成初始设置。
- 设置环境变量 `ANDROID_HOME` 为 Android SDK 文件夹。此路径可以在 Android Studio 设置中找到。
- 设置 JDK：
  - 推荐选项是设置环境变量 `ANDROID_STUDIO` 为你的 Android Studio 安装路径。构建脚本将自动查找并使用捆绑的 JDK。
  - 你也可以自己设置 JDK 17，但本指南不会涵盖这些说明。
- 克隆源代码：`git clone --recurse-submodules https://github.com/topjohnwu/Magisk.git`
- 运行 `./build.py ndk` 让脚本为你下载并安装 NDK

## 构建

- 要构建所有内容并创建最终的 Magisk APK，运行 `./build.py all`。
- 你也可以构建特定的子组件；调用 `build.py` 查看你的选项。\
  对于每个操作，使用 `-h` 访问帮助（例如 `./build.py binary -h`）
- 使用 `config.prop` 配置构建。提供了示例 `config.prop.sample`。

## IDE 支持

- 项目中的 Kotlin、Java、C++ 和 C 代码应该在 Android Studio 中开箱即用。此存储库可以直接作为项目在 Android Studio 中打开。
- 有关 Rust 开发，请参阅下一节。
- 在处理任何本机代码之前，先使用 `./build.py binary` 构建所有本机代码，因为某些生成的代码仅在构建过程中创建。

### 开发 Rust

首先，安装 [rustup](https://www.rust-lang.org/tools/install)，官方 Rust 工具链管理器。Magisk NDK 包 [ONDK](https://github.com/topjohnwu/ondk)（使用 `./build.py ndk` 安装的那个）捆绑了一个完整的 Rust 工具链，因此_构建_ Magisk 项目本身不需要任何进一步配置。

但是，如果你想处理 Rust 代码库，将 ONDK 的 Rust 工具链链接到 `rustup` 并将其设置为默认值会更容易，这样多个开发工具和 IDE 将正常工作：

```bash
# 将 ONDK 工具链链接为 "magisk"
rustup toolchain link magisk "$ANDROID_HOME/ndk/magisk/toolchains/rust"
# 将 magisk 设置为默认
rustup default magisk
```

如果你计划使用 VSCode，你可以安装 [rust-analyzer](https://marketplace.visualstudio.com/items?itemName=rust-lang.rust-analyzer) 插件，一切应该都准备就绪。如果你计划使用 Jetbrain IDE（例如 [Rustrover](https://www.jetbrains.com/rust/) 或其 Rust 插件），我们需要一些额外设置：

- 安装官方 nightly 工具链并添加一些组件。我们实际上不会将 nightly 工具链用于任何事情，除了欺骗 IDE 配合；我们在下一步设置的包装器中进行实际工作。

```bash
rustup toolchain install nightly
# 添加一些 ONDK 中也包含的组件
rustup +nightly component add rust-src clippy
```

- 创建一个包装器 cargo bin 目录来解决 `rustup` 限制

```bash
# 我们这里选择 ~/.cargo/wrapper 作为示例（也是一个好的推荐）
# 选择你喜欢的任何路径，你只需要在下一步中使用此路径
./build.py rustup ~/.cargo/wrapper
```

- 在 Settings > Rust > Toolchain location 中，将其设置为我们刚创建的包装器目录的路径。
- IDE 现在应该完全功能正常，你可以启用 `rustfmt` 并使用 `Clippy` 作为外部 linter。

## 签名和分发

- 在发布版本中，签名 Magisk APK 的密钥证书将被 Magisk 的 root 守护进程用作参考，以拒绝并强制卸载任何不匹配的 Magisk 应用，保护用户免受恶意和未经验证的 Magisk APK 的侵害。
- 要对 Magisk 本身进行任何开发，请切换到**官方调试版本并重新安装 Magisk** 以关闭签名检查。
- 要分发使用你自己密钥签名的你自己的 Magisk 构建版本，请在 `config.prop` 中设置你的签名配置。
- 查看 [Google 的文档](https://developer.android.com/studio/publish/app-signing.html#generate-key) 了解有关生成你自己密钥的更多详细信息。
