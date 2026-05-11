# 安装

如果你已经安装了 Magisk，**强烈建议**通过 Magisk 应用使用"直接安装"方法进行升级。以下教程仅适用于初次安装。

## 开始之前

开始之前：

- 本教程假设你了解如何使用 `adb` 和 `fastboot`
- 如果你计划同时安装自定义内核，请在 Magisk 之后安装
- 你的设备引导加载程序必须已解锁

---

下载并安装最新的 [Magisk 应用](https://github.com/topjohnwu/Magisk/releases/latest)。在主屏幕上，你应该看到：

<p align="center"><img src="/device_info.png" width="500"/></p>

**Ramdisk** 的结果决定了你的设备在 boot 分区中是否有 ramdisk。如果你的设备没有 boot ramdisk，请在继续之前阅读 [Magisk 在 Recovery 中](#magisk-在-recovery-中)部分。

> _(不幸的是，有些设备的引导加载程序即使不应该也会接受 ramdisk。在这种情况下，你必须按照设备 boot 分区**确实**包含 ramdisk 的说明进行操作。无法检测到这一点，唯一确定的方法是实际尝试。幸运的是，据我们所知，只有某些小米设备具有此属性，因此大多数人可以忽略此信息。)_

如果你使用的是三星设备，现在可以跳转到[专属部分](#三星设备)。

如果你的设备有 boot ramdisk，请获取 `boot.img`（或 `init_boot.img`，如果存在）的副本。<br>
如果你的设备**没有** boot ramdisk，请获取 `recovery.img` 的副本。<br>
你应该能够从官方固件包或自定义 ROM zip 中提取所需文件。

快速回顾，此时你应该已经了解并准备好了：

1. 你的设备是否有 boot ramdisk
2. 基于 (1) 准备一个 `boot.img`、`init_boot.img` 或 `recovery.img`

让我们继续到[修补镜像](#修补镜像)。

## 修补镜像

- 将 boot/init_boot/recovery 镜像复制到你的设备
- 按 Magisk 卡片中的**安装**按钮
- 如果你正在修补 recovery 镜像，请勾选**"Recovery 模式"**选项
- 选择**"选择并修补文件"**作为方法，然后选择 boot/init_boot/recovery 镜像
- 开始安装，然后使用 ADB 将修补后的镜像复制到你的电脑：<br>
  `adb pull /sdcard/Download/magisk_patched_[random_strings].img`
- 将修补后的 boot/init_boot/recovery 镜像刷入你的设备；<br>
  对于大多数设备，重启进入 fastboot 模式并使用命令刷入：<br>
  `fastboot flash boot /path/to/magisk_patched_[random_strings].img` 或 <br>
  `fastboot flash init_boot /path/to/magisk_patched_[random_strings].img` 或 <br>
  `fastboot flash recovery /path/to/magisk_patched_[random_strings].img` <br>
- （可选）如果你的设备有单独的 `vbmeta` 分区，你可以使用以下命令修补 `vbmeta` 分区：<br>
  `fastboot flash vbmeta --disable-verity --disable-verification vbmeta.img`（注意这可能会**擦除你的数据**）
- 重启并启动 Magisk 应用（如果你已擦除数据，你会看到一个存根 Magisk 应用；使用它来引导到完整的 Magisk 应用），你会看到一个提示要求进行环境修复；点击并等待重启
- 完成！

> 警告：**永远不要**刷入他人分享的修补镜像或在另一台设备上修补镜像，即使它们具有相同的设备型号！你可能需要完全擦除数据才能恢复设备。**始终**在**你要安装 Magisk 的同一台设备上**修补 boot 镜像。

## 卸载

卸载 Magisk 最简单的方法是直接通过 Magisk 应用。如果你坚持使用自定义 recovery，请将 Magisk APK 重命名为 `uninstall.zip`，然后像刷入任何其他普通可刷入 zip 一样刷入它。

## Magisk 在 Recovery 中

当你的设备在 boot 镜像中没有 ramdisk 时，Magisk 别无选择，只能劫持 recovery 分区。对于这些设备，每次你想启用 Magisk 时都必须**重启到 recovery**。

当 Magisk 劫持 recovery 时，有一个特殊机制允许你_实际_启动到 recovery 模式。每个设备型号都有自己的按键组合来启动到 recovery，例如 Galaxy S10 是（电源 + Bixby + 音量上）。在线快速搜索应该很容易找到这些信息。一旦你按下按键组合并且设备振动并显示启动画面，松开所有按钮即可启动到 Magisk。如果你决定启动到实际的 recovery 模式，**长按音量上直到你看到 recovery 界面**。

总结，在 recovery 中安装 Magisk 后**（从关机状态开始）**：

- **（正常开机）→（没有 Magisk 的系统）**
- **（Recovery 按键组合）→（启动画面）→（松开所有按钮）→（有 Magisk 的系统）**
- **（Recovery 按键组合）→（启动画面）→（长按音量上）→（Recovery 模式）**

（注意：在这种情况下你**不能**使用自定义 recovery 来安装或升级 Magisk！！）

## 三星设备

在继续之前，请确认：

- 安装 Magisk **会**触发你的 Knox 保修位，此操作不可逆。
- 首次安装 Magisk **需要**完全擦除数据（这**不**包括解锁引导加载程序时的数据擦除）。请备份你的数据。

### 刷入工具

- [Samsung Odin3](https://dl2018.sammobile.com/Odin.zip)（仅限 Windows）（需要 [Samsung USB 驱动程序](https://developer.samsung.com/android-usb-driver)）
- [Samsung Odin4](https://forum.xda-developers.com/t/official-samsung-odin-v4-1-2-1-dc05e3ea-for-linux.4453423/)（仅限 Linux）
- [Heimdall](https://www.glassechidna.com.au/heimdall/)（或 [Grimler 的分支](https://git.sr.ht/~grimler/Heimdall)）

### 要求

要验证是否可以在你的三星设备上安装 Magisk，你必须首先检查 OEM Lock 和 KnoxGuard (RMM) 状态。为此，请进入 `设置` -> `设备维护` -> `维护模式` 并启用它，然后使用按键组合启动设备进入下载模式。

可能的 OEM Lock 值如下：
- **ON (L)**：完全锁定。
- **ON (U)**：引导加载程序锁定，OEM 解锁已启用。
- **OFF (U)**：完全解锁。

要解锁你的引导加载程序，请按照以下说明操作。如果在下载模式中没有显示 OEM Lock 值，你的设备可能由于市场限制（美国/加拿大设备）而无法解锁。

可能的 KnoxGuard 值如下：

- `Active`、`Locked`：你的设备已被电信运营商或保险公司远程锁定。
- `Prenormal`：你的设备暂时锁定，达到 168 小时的运行时间应触发解锁。
- `Checking`、`Completed`、`Broken`：你的设备已解锁。

KnoxGuard 处于活动状态将阻止你安装/运行 Magisk，无论你的引导加载程序锁定状态如何。

### 解锁引导加载程序

- 在**开发者选项 → OEM 解锁**中允许引导加载程序解锁
- 重启到下载模式：关闭设备电源，然后按设备的下载模式按键组合
- 长按音量上解锁引导加载程序。**这将擦除你的数据并自动重启。**
- 完成初始设置。跳过所有步骤，因为数据将在后续步骤中再次擦除。**在设置过程中连接设备到互联网。**
- 启用开发者选项，并**确认 OEM 解锁选项存在且为灰色。**这意味着 KnoxGuard 没有锁定你的设备。
- 你的引导加载程序现在接受下载模式中的非官方镜像

### 说明

- 下载你设备的最新固件包，你可以使用以下工具之一直接从三星服务器下载：
  - [SamFirm.NET](https://github.com/jesec/SamFirm.NET)、[samfirm.js](https://github.com/jesec/samfirm.js)
  - [Frija](https://forum.xda-developers.com/s10-plus/how-to/tool-frija-samsung-firmware-downloader-t3910594)
  - [Samloader](https://forum.xda-developers.com/s10-plus/how-to/tool-samloader-samfirm-frija-replacement-t4105929)
  - [Bifrost](https://forum.xda-developers.com/t/tool-samsung-samsung-firmware-downloader.4240719/)
- 解压固件并将 `AP` tar 文件复制到你的设备。通常命名为 `AP_[device_model_sw_ver].tar.md5`
- 按 Magisk 卡片中的**安装**按钮
- 如果你的设备**没有** boot ramdisk，请勾选**"Recovery 模式"**选项
- 选择**"选择并修补文件"**作为方法，然后选择 `AP` tar 文件
- 开始安装，然后使用 ADB 将修补后的 tar 文件复制到你的电脑：<br>
  `adb pull /sdcard/Download/magisk_patched_[random_strings].tar`<br>
  **不要使用 MTP**，因为已知它会损坏大文件。
- 重启到下载模式。在你的电脑上打开 Odin，将 `magisk_patched.tar` 作为 `AP` 刷入，连同原始固件中的 `BL`、`CP` 和 `CSC`（**不是** `HOME_CSC`，因为我们要**擦除数据**）。
- Odin 完成刷入后，你的设备应该会自动重启。**如果要求进行出厂重置，请同意。**
- 如果你的设备**没有** boot ramdisk，现在重启到 recovery 以启用 Magisk（原因见 [Magisk 在 Recovery 中](#magisk-在-recovery-中)）。
- 安装你已经下载的 Magisk 应用并启动应用。它应该会显示一个对话框要求额外设置。
- 让应用完成它的工作并自动重启设备。完成！

### 升级操作系统

一旦你 root 了三星设备，你就不能再通过 OTA 升级你的 Android 操作系统。要升级设备的操作系统，你必须手动下载新的固件 zip 文件，并按照上一节中编写的相同 `AP` 修补过程进行操作。**唯一的区别在于 Odin 刷入步骤：不要使用 `CSC` tar，而是使用 `HOME_CSC` tar，因为我们要执行的是升级，而不是初次安装**。

### 重要提示

- **永远不要**尝试将 `boot`、`init_boot`、`recovery` 或 `vbmeta` 分区恢复到原始状态！这样做可能会导致设备变砖，唯一恢复的方法是使用数据擦除进行完全 Odin 恢复。
- 要使用新固件升级设备，**永远不要**直接使用原始 `AP` tar 文件，原因如上所述。**始终**在 Magisk 应用中修补 `AP` 并使用修补后的版本。

## 自定义 Recovery

> **此安装方法已弃用，仅以最低限度维护。你已被警告！**

使用自定义 recovery 安装仅在你的设备有 boot ramdisk 时才可能。在现代设备上通过自定义 recovery 安装 Magisk 不再推荐。如果你遇到任何问题，请使用[修补镜像](#修补镜像)方法。

- 下载 Magisk APK
- 将 `.apk` 文件扩展名重命名为 `.zip`，例如：`Magisk-v24.0.apk` → `Magisk-v24.0.zip`。如果你在重命名文件扩展名时遇到困难（如在 Windows 上），请使用 Android 上的文件管理器或自定义 recovery 中包含的文件管理器来重命名文件。
- 像刷入任何其他普通可刷入 zip 一样刷入该 zip。
- 重启并检查 Magisk 应用是否已安装。如果没有自动安装，请手动安装 APK。
- 启动 Magisk 应用；它将显示一个对话框要求重新安装。**直接在应用内**进行重新安装并重启（如果你使用的是在启动后锁定 boot 分区的 MTK 设备，请[修补 boot 镜像](#修补镜像)并通过自定义 recovery 或 fastboot 刷入）。

> 警告：模块的 `sepolicy.rule` 文件可能存储在 `cache` 分区中。不要擦除 `cache` 分区。
