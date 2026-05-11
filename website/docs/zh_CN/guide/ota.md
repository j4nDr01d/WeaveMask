## OTA 升级指南
Magisk 不会修改大多数只读分区，这意味着应用官方 OTA 要简单得多。以下是几种不同类型设备应用 OTA 并在安装后保留 Magisk 的教程（如果可能）。这只是一个通用指南，因为每个设备的程序可能有所不同。

**注意：为了应用 OTA，你必须确保自己没有以任何方式修改只读分区（如 `/system` 或 `/vendor`）。即使将分区重新挂载为 rw 也会篡改块验证！！**

### 前提条件
- 请在开发者选项中禁用*自动系统更新*，这样它就不会在你不知情的情况下安装 OTA。

<p align="center"><img src="/disable_auto_ota.png" width="250"/></p>

- 当 OTA 可用时，首先进入（Magisk 应用 → 卸载 → 恢复镜像）。**不要重启，否则 Magisk 将被卸载。**这将把 Magisk 修改的分区从安装时的备份恢复到原始状态，以通过 OTA 前的块验证。**在执行以下任何步骤之前，此步骤是必需的！**

<p align="center"><img src="/restore_img.png" width="300"/></p>

### A/B 分区设备

可以将 OTA 安装到非活动槽位，并让 Magisk 应用将 Magisk 安装到更新的分区上。开箱即用的 OTA 安装可以无缝工作，安装后可以保留 Magisk。

- 恢复原始镜像后，像平常一样应用 OTA（设置 → 系统 → 系统更新）。
- 等待安装完全完成（OTA 的两个步骤：第 1 步"安装更新"和第 2 步"优化设备"），**不要按"立即重启"或"重启"按钮！**而是进入（Magisk 应用 → 安装 → 安装到非活动槽位）将 Magisk 安装到更新的槽位。

<p align="center"><img src="/ota_done.png" width="250"/> <img src="/install_inactive_slot.png" width="250"/></p>

- 安装完成后，按照 Magisk 安装结束时的最终说明操作，了解如何重启到新槽位，目前涉及返回常规系统更新并点击"立即重启"（之前在 Magisk 应用内使用重启的方法可能不会导致重启到新槽位）。在幕后，Magisk 应用跟踪你的设备切换到更新的槽位，绕过任何可能的 OTA 后验证。

<p align="center"><img src="/manager_reboot.png" width="250"/></p>

### "非 A/B"设备
不幸的是，对于这些设备，没有真正好的方法来应用 OTA。以下教程不会保留 Magisk；你必须在升级后手动重新 root 你的设备，这需要访问计算机。这些只是"最佳实践"。

- 要正确安装 OTA，你的设备上必须安装了原始 recovery。如果你安装了自定义 recovery，你可以从之前的备份、在线找到的转储或 OEM 提供的出厂镜像恢复它。
如果你决定在不接触 recovery 分区的情况下安装 Magisk，你有几个选择，无论哪种方式你都会得到一个已 root 的 Magisk 设备，但 recovery 保持原始状态不受影响：
    - 如果支持，使用 `fastboot boot <recovery_img>` 启动自定义 recovery 并安装 Magisk。
    - 如果你有原始镜像转储的副本，使用 Magisk 应用的"修补镜像"功能安装 Magisk
- 一旦你恢复到原始 recovery 和其他镜像，下载 OTA。可选地，一旦你下载了 OTA 更新 zip，找到一种方法解压 zip（因为通常涉及 root）
- 应用 OTA 并重启设备。这将使用设备的官方原始 OTA 安装机制来升级你的系统。
- 完成后，你将得到一个已升级的、100% 原始的、未 root 的设备。你必须手动刷回 Magisk。考虑使用第 1 步中提到的方法在不接触 recovery 分区的情况下刷入 Magisk，如果你想频繁接收原始 OTA。
