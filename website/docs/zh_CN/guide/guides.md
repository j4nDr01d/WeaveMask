# 开发者指南

## BusyBox

Magisk 附带一个功能完整的 BusyBox 二进制文件（包括完整的 SELinux 支持）。可执行文件位于 `/data/adb/magisk/busybox`。Magisk 的 BusyBox 支持运行时可切换的"ASH 独立 Shell 模式"。这种独立模式意味着在 BusyBox 的 `ash` shell 中运行时，每个命令都将直接使用 BusyBox 中的小程序，无论 `PATH` 设置为什么。例如，`ls`、`rm`、`chmod` 等命令将**不会**使用 `PATH` 中的内容（在 Android 的情况下默认为 `/system/bin/ls`、`system/bin/rm` 和 `/system/bin/chmod`），而是直接调用内部的 BusyBox 小程序。这确保脚本始终在可预测的环境中运行，并且无论运行的 Android 版本如何，始终拥有完整的命令套件。要强制命令_不_使用 BusyBox，你必须使用完整路径调用可执行文件。

在 Magisk 上下文中运行的每个 shell 脚本都将在启用独立模式的 BusyBox `ash` shell 中执行。对于与第三方开发者相关的内容，这包括所有启动脚本和模块安装脚本。

对于那些想在 Magisk 之外使用此"独立模式"功能的人，有 2 种启用方式：

1. 设置环境变量 `ASH_STANDALONE` 为 `1`<br>示例：`ASH_STANDALONE=1 /data/adb/magisk/busybox sh <script>`
2. 使用命令行选项切换：<br>`/data/adb/magisk/busybox sh -o standalone <script>`

要确保所有后续执行的 `sh` shell 也在独立模式下运行，选项 1 是首选方法（这也是 Magisk 和 Magisk 应用内部使用的方式），因为环境变量会继承到子进程。

## Magisk 模块

Magisk 模块是放置在 `/data/adb/modules` 中的文件夹，结构如下：

```
/data/adb/modules
├── .
├── .
|
├── $MODID                  <--- 文件夹以模块的 ID 命名
│   │
│   │      *** 模块标识 ***
│   │
│   ├── module.prop         <--- 此文件存储模块的元数据
│   │
│   │      *** 主要内容 ***
│   │
│   ├── system              <--- 如果 skip_mount 不存在，此文件夹将被挂载
│   │   ├── ...
│   │   ├── ...
│   │   └── ...
│   │
│   ├── zygisk              <--- 此文件夹包含模块的 Zygisk 本机库
│   │   ├── arm64-v8a.so
│   │   ├── armeabi-v7a.so
│   │   ├── riscv64.so
│   │   ├── x86.so
│   │   ├── x86_64.so
│   │   └── unloaded        <--- 如果存在，本机库不兼容
│   │
│   │      *** 状态标志 ***
│   │
│   ├── skip_mount          <--- 如果存在，Magisk 将不会挂载你的 system 文件夹
│   ├── disable             <--- 如果存在，模块将被禁用
│   ├── remove              <--- 如果存在，模块将在下次重启时被移除
│   │
│   │      *** 可选文件 ***
│   │
│   ├── post-fs-data.sh     <--- 此脚本将在 post-fs-data 中执行
│   ├── service.sh          <--- 此脚本将在 late_start service 中执行
|   ├── uninstall.sh        <--- 当 Magisk 移除你的模块时，此脚本将被执行
|   ├── action.sh           <--- 当用户在 Magisk 应用中点击操作按钮时，此脚本将被执行
│   ├── system.prop         <--- 此文件中的属性将被 resetprop 作为系统属性加载
│   ├── sepolicy.rule       <--- 附加的自定义 sepolicy 规则
│   │
│   │      *** 自动生成，请勿手动创建或修改 ***
│   │
│   ├── vendor              <--- 指向 $MODID/system/vendor 的符号链接
│   ├── product             <--- 指向 $MODID/system/product 的符号链接
│   ├── system_ext          <--- 指向 $MODID/system/system_ext 的符号链接
│   │
│   │      *** 允许任何额外的文件/文件夹 ***
│   │
│   ├── ...
│   └── ...
|
├── another_module
│   ├── .
│   └── .
├── .
├── .
```

#### module.prop

这是 `module.prop` 的**严格**格式

```
id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>
updateJson=<url> (可选)
```

- `id` 必须匹配此正则表达式：`^[a-zA-Z][a-zA-Z0-9._-]+$`<br>
  例如：✓ `a_module`、✓ `a.module`、✓ `module-101`、✗ `a module`、✗ `1_module`、✗ `-a-module`<br>
  这是你的模块的**唯一标识符**。一旦发布就不应更改。
- `versionCode` 必须是**整数**。这用于比较版本
- `updateJson` 应指向一个 URL，下载 JSON 以提供信息，以便 Magisk 应用可以更新模块。
- 上面未提及的其他内容可以是任何**单行**字符串。
- 确保使用 `UNIX (LF)` 换行类型，而不是 `Windows (CR+LF)` 或 `Macintosh (CR)`。

更新 JSON 格式：

```
{
    "version": string,
    "versionCode": int,
    "zipUrl": url,
    "changelog": url
}
```

#### Shell 脚本 (`*.sh`)

请阅读[启动脚本](#启动脚本)部分以了解 `post-fs-data.sh` 和 `service.sh` 之间的区别。对于大多数模块开发者来说，如果你只需要运行启动脚本，`service.sh` 应该足够了。如果你需要等待启动完成，可以使用 `resetprop -w sys.boot_completed 0`。

在模块的所有脚本中，请使用 `MODDIR=${0%/*}` 获取模块的基础目录路径；**不要**在脚本中硬编码你的模块路径。
如果启用了 Zygisk，环境变量 `ZYGISK_ENABLED` 将被设置为 `1`。

#### `system` 文件夹

所有你想要替换/注入的文件应放置在此文件夹中。此文件夹将递归合并到真实的 `/system` 中；即：真实 `/system` 中的现有文件将被模块的 `system` 中的文件替换，模块的 `system` 中的新文件将被添加到真实的 `/system`。

如果你在任何文件夹中放置名为 `.replace` 的文件，该文件夹将直接替换真实系统中的文件夹，而不是合并其内容。这对于替换整个文件夹非常方便。

如果你想替换 `/vendor`、`/product` 或 `/system_ext` 中的文件，请将它们分别放在 `system/vendor`、`system/product` 和 `system/system_ext` 下。Magisk 将透明处理这些分区是否在单独的分区中。

如果你想移除特定的文件或文件夹，请在相同路径放置一个主设备号为 0、次设备号为 0 的虚拟字符设备。例如，如果你想移除 `/system/app/GoogleCamera`，你可以在 `$MODDIR/system/app` 中执行 `mknod GoogleCamera c 0 0`。

#### Zygisk

Zygisk 是 Magisk 的一个功能，允许高级模块开发者在每个 Android 应用程序进程被专门化和运行之前直接在其中运行代码。有关 Zygisk API 和构建 Zygisk 模块的更多详细信息，请查看 [Zygisk 模块示例](https://github.com/topjohnwu/zygisk-module-sample)项目。

#### system.prop

此文件遵循与 `build.prop` 相同的格式。每行包含 `[key]=[value]`。

#### sepolicy.rule

如果你的模块需要一些额外的 sepolicy 补丁，请将这些规则添加到此文件中。此文件中的每一行都将被视为策略语句。有关策略语句格式的更多详细信息，请检查 [magiskpolicy](/zh_CN/guide/tools#magiskpolicy) 的文档。

## Magisk 模块安装器

Magisk 模块安装器是一个打包在 zip 文件中的 Magisk 模块，可以在 Magisk 应用或自定义 recovery（如 TWRP）中刷入。最简单的 Magisk 模块安装器就是一个打包为 zip 的 Magisk 模块，此外仅在模块支持在 recovery 中刷入时添加以下文件：

- `update-binary`：下载最新的 [module_installer.sh](https://github.com/topjohnwu/Magisk/blob/master/scripts/module_installer.sh) 并将该脚本重命名/复制为 `update-binary`
- `updater-script`：此文件应仅包含字符串 `#MAGISK`

模块安装器脚本将设置环境，从 zip 文件中提取模块文件到正确的位置，然后完成安装过程，这对于大多数简单的 Magisk 模块来说应该足够了。

```
module.zip
│
├── META-INF                           <--- 仅在 recovery 中刷入时需要
│   └── com
│       └── google
│           └── android
│               ├── update-binary      <--- 你下载的 module_installer.sh
│               └── updater-script     <--- 应仅包含字符串 "#MAGISK"
│
├── customize.sh                       <--- （可选，更多详情稍后）
│                                           此脚本将被 update-binary 源引用
├── ...
├── ...  /* 模块的其余文件 */
│
```

#### 自定义

如果你需要自定义模块安装过程，可以选择在安装器中创建一个名为 `customize.sh` 的脚本。此脚本将在所有文件被提取并应用默认权限和 secontext 后被模块安装器脚本_源引用_（不是执行！）。如果你的模块需要基于设备 ABI 的额外设置，或者你需要为某些模块文件设置特殊权限/secontext，这非常有用。

如果你想要完全控制和自定义安装过程，在 `customize.sh` 中声明 `SKIPUNZIP=1` 以跳过所有默认安装步骤。这样做后，你的 `customize.sh` 将负责自行安装所有内容。

`customize.sh` 脚本在 Magisk 的 BusyBox `ash` shell 中运行，启用"独立模式"。以下变量和函数可用：

##### 变量

- `MAGISK_VER` (string)：当前安装的 Magisk 的版本字符串（例如 `v20.0`）
- `MAGISK_VER_CODE` (int)：当前安装的 Magisk 的版本代码（例如 `20000`）
- `BOOTMODE` (bool)：如果模块正在 Magisk 应用中安装则为 `true`
- `MODPATH` (path)：你的模块文件应安装的路径
- `TMPDIR` (path)：你可以临时存储文件的地方
- `ZIPFILE` (path)：你的模块安装 zip
- `ARCH` (string)：设备的 CPU 架构。值为 `arm`、`arm64`、`x86`、`x64` 或 `riscv64`
- `IS64BIT` (bool)：如果 `$ARCH` 是 `arm64`、`x64` 或 `riscv64` 则为 `true`
- `API` (int)：设备的 API 级别（Android 版本）（例如 Android 6.0 为 `23`）

##### 函数

```
ui_print <msg>
    将 <msg> 打印到控制台
    避免使用 'echo'，因为它不会显示在自定义 recovery 的控制台中

abort <msg>
    将错误消息 <msg> 打印到控制台并终止安装
    避免使用 'exit'，因为它会跳过终止清理步骤

set_perm <target> <owner> <group> <permission> [context]
    如果未指定 [context]，默认为 "u:object_r:system_file:s0"
    此函数是以下命令的简写：
       chown owner.group target
       chmod permission target
       chcon context target

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    如果未指定 [context]，默认为 "u:object_r:system_file:s0"
    此函数是以下伪代码的简写：
      set_perm <directory> owner group dirpermission context
      for file in <directory>:
        set_perm file owner group filepermission context
      for dir in <directory>:
        set_perm_recursive dir owner group dirpermission context
```

为方便起见，你还可以在变量名 `REPLACE` 中声明要替换的文件夹列表。模块安装器脚本将在 `REPLACE` 中列出的文件夹中创建 `.replace` 文件。例如：

```sh
REPLACE="
/system/app/YouTube
/system/app/Bloatware
"
```

上面的列表将导致创建以下文件：`$MODPATH/system/app/YouTube/.replace` 和 `$MODPATH/system/app/Bloatware/.replace`。

为方便起见，你还可以在变量名 `REMOVE` 中声明要移除的文件/文件夹列表。模块安装器脚本将创建相应的虚拟设备。例如：

```sh
REMOVE="
/system/app/YouTube
/system/fonts/Roboto.ttf
"
```

上面的列表将导致创建以下虚拟设备：`$MODPATH/system/app/YouTube` 和 `$MODPATH/system/fonts/Roboto.ttf`。

#### 注意事项

- 当你的模块通过 Magisk 应用下载时，`update-binary` 将被**强制**替换为最新的 [`module_installer.sh`](https://github.com/topjohnwu/Magisk/blob/master/scripts/module_installer.sh)。**不要**尝试在 `update-binary` 中添加任何自定义逻辑。
- 由于历史原因，**不要**在模块安装器 zip 中添加名为 `install.sh` 的文件。
- **不要**在 `customize.sh` 的末尾调用 `exit`。模块安装器脚本必须在退出前执行一些清理工作。

## 启动脚本

在 Magisk 中，你可以以 2 种不同的模式运行启动脚本：**post-fs-data** 和 **late_start service** 模式。

- post-fs-data 模式
  - 此阶段是阻塞的。启动过程在执行完成或 40 秒过去之前暂停。
  - 脚本在任何模块挂载之前运行。这允许模块开发者在模块挂载之前动态调整他们的模块。
  - 此阶段发生在 Zygote 启动之前，这几乎意味着 Android 中的一切
  - **警告：**使用 `setprop` 将导致启动过程死锁！请改用 `resetprop -n <prop_name> <prop_value>`。
  - **仅在必要时在此模式下运行脚本。**
- late_start service 模式
  - 此阶段是非阻塞的。你的脚本与启动过程的其余部分并行运行。
  - **这是运行大多数脚本的推荐阶段。**

在 Magisk 中，还有 2 种脚本：**通用脚本**和**模块脚本**。

- 通用脚本
  - 放置在 `/data/adb/post-fs-data.d` 或 `/data/adb/service.d` 中
  - 仅在脚本设置为可执行时执行（`chmod +x script.sh`）
  - `post-fs-data.d` 中的脚本在 post-fs-data 模式下运行，`service.d` 中的脚本在 late_start service 模式下运行。
  - 模块在安装期间**不应**添加通用脚本
- 模块脚本
  - 放置在模块自己的文件夹中
  - 仅在模块启用时执行
  - `post-fs-data.sh` 在 post-fs-data 模式下运行，`service.sh` 在 late_start service 模式下运行。

所有启动脚本都将在启用"独立模式"的 Magisk 的 BusyBox `ash` shell 中运行。

## 根目录覆盖系统

由于 `/` 在 system-as-root 设备上是只读的，Magisk 提供了一个覆盖系统，使开发者能够替换 rootdir 中的文件或添加新的 `*.rc` 脚本。此功能主要为自定义内核开发者设计。

覆盖文件应放置在 boot 镜像 ramdisk 中的 `overlay.d` 文件夹中，它们遵循以下规则：

1. `overlay.d` 中的每个 `*.rc` 文件（`init.rc` 除外）将在 `init.rc` 之后被读取和连接（如果它不存在于根目录中），否则它将**替换**现有的文件。
2. 现有文件可以被位于相同相对路径的文件替换
3. 对应于不存在的文件的文件将被忽略

要添加可以在你的自定义 `*.rc` 脚本中引用的额外文件，请将它们添加到 `overlay.d/sbin` 中。上面的 3 条规则不适用于此文件夹中的任何内容；相反，它们将被直接复制到 Magisk 的内部 `tmpfs` 目录（过去总是在 `/sbin`）。

从 Android 11 开始，`/sbin` 文件夹可能不再存在，在这种情况下，Magisk 使用 `/debug_ramdisk` 代替。`*.rc` 脚本中模式 `${MAGISKTMP}` 的每个出现都将被 `magiskinit` 注入到 `init.rc` 时替换为 Magisk `tmpfs` 文件夹。在 Android 11 之前的设备上，`${MAGISKTMP}` 将简单地被替换为 `/sbin`，因此在引用这些额外文件时**永远不要**在 `*.rc` 脚本中硬编码 `/sbin`。

以下是设置 `overlay.d` 和自定义 `*.rc` 脚本的示例：

```
ramdisk
│
├── overlay.d
│   ├── sbin
│   │   ├── libfoo.ko      <--- 这 2 个文件将被复制
│   │   └── myscript.sh    <--- 到 Magisk 的 tmpfs 目录
│   ├── custom.rc          <--- 此文件将被注入到 init.rc
│   ├── res
│   │   └── random.png     <--- 此文件将替换 /res/random.png
│   └── new_file           <--- 此文件将被忽略，因为
│                               /new_file 不存在
├── res
│   └── random.png         <--- 此文件将被
│                               /overlay.d/res/random.png 替换
├── ...
├── ...  /* initramfs 文件的其余部分 */
│
```

以下是 `custom.rc` 的示例：

```
# 使用 ${MAGISKTMP} 引用 Magisk 的 tmpfs 目录

on early-init
    setprop sys.example.foo bar
    insmod ${MAGISKTMP}/libfoo.ko
    start myservice

service myservice ${MAGISKTMP}/myscript.sh
    oneshot
```
