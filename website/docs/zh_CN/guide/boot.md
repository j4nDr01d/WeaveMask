# Android 启动的那些事

## 术语

- **rootdir**：根目录（`/`）。所有文件/文件夹/文件系统都存储在或挂载在 rootdir 下。在 Android 上，文件系统可能是 `rootfs` 或 `system` 分区。
- **`initramfs`**：Android boot 镜像中 Linux 内核将用作 `rootfs` 的部分。人们也互换使用术语 **ramdisk**
- **`recovery` 和 `boot` 分区**：这 2 个实际上非常相似：两者都是包含 ramdisk 和 Linux 内核（加上一些其他内容）的 Android boot 镜像。唯一的区别是启动 `boot` 分区将带我们进入 Android，而 `recovery` 有一个用于修复和升级设备的极简独立 Linux 环境。
- **SAR**：System-as-root。即设备使用 `system` 作为 rootdir 而不是 `rootfs`
- **A/B, A-only**：对于支持[无缝系统更新](https://source.android.com/devices/tech/ota/ab)的设备，它将有 2 个所有只读分区的槽位；我们称这些为 **A/B 设备**。为了区分，非 A/B 设备将被称为 **A-only**
- **2SI**：Two Stage Init。Android 10+ 的启动方式。更多信息稍后。

以下是更精确定义设备 Android 版本的几个参数：

- **LV**：Launch Version。设备**发布时**的 Android 版本。即设备首次上市时预装的 Android 版本。
- **RV**：Running Version。设备**当前运行**的 Android 版本。

我们将使用 **Android API 级别**来表示 LV 和 RV。API 级别和 Android 版本之间的映射可以在[此表](https://source.android.com/setup/start/build-numbers#platform-code-names-versions-api-levels-and-ndk-releases)中看到。例如：Pixel XL 发布时搭载 Android 7.1，正在运行 Android 10，这些参数将是 `(LV = 25, RV = 29)`

## 启动方法

Android 启动可以大致分为 3 种主要不同的方法。我们提供一个通用经验法则来确定你的设备最可能使用哪种方法，并单独列出例外情况。

方法 | 初始 rootdir | 最终 rootdir
:---: | --- | ---
**A** | `rootfs` | `rootfs`
**B** | `system` | `system`
**C** | `rootfs` | `system`

- **方法 A - 旧版 ramdisk**：这是_所有_ Android 设备过去启动的方式（美好的旧时光）。内核使用 `initramfs` 作为 rootdir，并执行 `/init` 启动。
	- 不符合方法 B 和 C 标准的任何设备
- **方法 B - 旧版 SAR**：此方法首次出现在 Pixel 1 上。内核直接将 `system` 分区挂载为 rootdir 并执行 `/init` 启动。
	- 具有 `(LV = 28)` 的设备
	- Google：Pixel 1 和 2。Pixel 3 和 3a 当 `(RV = 28)` 时。
	- OnePlus：6 - 7
	- 可能一些 `(LV < 29)` Android Go 设备？
- **方法 C - 2SI ramdisk SAR**：此方法首次出现在 Pixel 3 Android 10 开发者预览中。内核使用 `initramfs` 作为 rootdir 并在 `rootfs` 中执行 `/init`。此 `init` 负责挂载 `system` 分区并将其用作新的 rootdir，然后最终执行 `/system/bin/init` 启动。
	- 具有 `(LV >= 29)` 的设备
	- 具有 `(LV < 28, RV >= 29)` 的设备，排除那些已经使用方法 B 的设备
	- Google：Pixel 3 和 3a 当 `(RV >= 29)` 时

### 讨论

从在线文档来看，Google 对 SAR 的定义仅考虑内核如何启动设备（上表中的**初始 rootdir**），这意味着只有使用**方法 B** 的设备才被 Google *官方*视为 SAR 设备。

然而对于 Magisk，真正的区别在于设备完全启动时最终使用什么（上表中的**最终 rootdir**），这意味着**就 Magisk 而言，方法 B 和 C 都是 SAR 的一种形式**，只是实现方式不同。本文档后面提到的每个 SAR 实例都将指**Magisk 的定义**，除非特别说明。

方法 C 的标准有点复杂，通俗地说：要么你的设备足够现代，出厂时搭载 Android 10+，要么你在一个使用方法 A 的设备上运行 Android 10+ 自定义 ROM。

- 任何运行 Android 10+ 的方法 A 设备将自动使用方法 C
- **方法 B 设备卡在方法 B**，唯一的例外是 Pixel 3 和 3a，Google 改造了设备以适应新方法。

SAR 是 [Project Treble](https://source.android.com/devices/architecture#hidl) 的重要组成部分，因为 rootdir 应该与平台绑定。这也是方法 B 和 C 带有 `(LV >= ver)` 标准的原因，因为 Google 每年都强制所有 OEM 遵守更新的要求。

## 一些历史

当 Google 发布第一代 Pixel 时，它还引入了 [A/B（无缝）系统更新](https://source.android.com/devices/tech/ota/ab)。由于[存储大小问题](https://source.android.com/devices/tech/ota/ab/ab_faqs)，与 A-only 相比有几个差异，最相关的是移除了 `recovery` 分区，recovery ramdisk 被合并到 `boot` 中。

让我们回到 Google 首次设计 A/B 的时候。如果使用 SAR（当时只有启动方法 B 存在），内核不需要 `initramfs` 来启动 Android（因为 rootdir 在 `system` 中）。这意味着我们可以聪明地将 recovery ramdisk（包含极简 Linux 环境）塞入 `boot`，移除 `recovery`，并让内核根据引导加载程序的信息选择使用哪个 rootdir（ramdisk 或 `system`）。

随着时间从 Android 7.1 推移到 Android 10，Google 引入了[动态分区](https://source.android.com/devices/tech/ota/dynamic_partitions/implement)。这对 SAR 来说是坏消息，因为 Linux 内核无法直接理解这种新的分区格式，因此无法直接将 `system` 挂载为 rootdir。这就是他们提出启动方法 C 的时候：始终启动到 `initramfs`，并让用户空间处理其余的启动过程。这包括决定是启动到 Android 还是 recovery，或者他们官方称呼的：`USES_RECOVERY_AS_BOOT`。

一些使用带有 2SI 的 A/B 的现代设备也带有 `recovery_a/_b` 分区。这在 Google 的标准中是官方支持的。这些设备将只使用 boot ramdisk 启动到 Android，因为 recovery 存储在单独的分区中。

## 拼凑在一起

有了以上所有知识，我们现在可以将所有 Android 设备分类为这些不同的类型：

类型 | 启动方法 | 分区 | 2SI | `boot` 中的 Ramdisk
:---: | :---: | :---: | :---: | :---:
**I** | A | A-only | 否 | `boot` ramdisk
**II** | B | A/B | 任意 | `recovery` ramdisk
**III** | B | A-only | 任意 | ***不可用***
**IV** | C | 任意 | 是 | 混合 ramdisk

这些类型按它们首次可用的时间顺序排列。

- **类型 I**：经典的旧版 ramdisk 启动
- **类型 II**：旧版 A/B 设备。Pixel 1 是此类型的第一个设备，既是第一个 A/B 也是第一个 SAR 设备
- **类型 III**：2018 年末 - 2019 年的 A-only 设备。**就 Magisk 而言，有史以来最糟糕的设备类型。**
- **类型 IV**：所有使用启动方法 C 的设备都是类型 IV。A/B 类型 IV ramdisk 可以根据引导加载程序的信息启动到 Android 或 recovery；A-only 类型 IV ramdisk 只能启动到 Android。

关于类型 III 设备的更多细节：Magisk 总是安装在 boot 镜像的 ramdisk 中。对于所有其他设备类型，因为它们的 `boot` 分区包含 ramdisk，Magisk 可以很容易地通过 Magisk 应用修补 boot 镜像或在自定义 recovery 中刷入 zip 来安装。然而对于类型 III 设备，它们**只能将 Magisk 安装到 `recovery` 分区**。Magisk 在正常启动时不会运行；相反，类型 III 设备所有者必须始终重启到 recovery 以保持 Magisk 访问。

一些类型 III 设备的引导加载程序仍然会接受并提供手动添加到 `boot` 镜像中的 `initramfs` 给内核（例如一些小米手机），但许多设备不会（例如三星 S10、Note 10）。这完全取决于 OEM 如何实现其引导加载程序。
