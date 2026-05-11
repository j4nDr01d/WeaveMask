# 内部细节

## 文件结构

### "Magisk tmpfs 目录"中的路径

Magisk 将挂载一个 `tmpfs` 目录来存储一些临时数据。对于有 `/sbin` 文件夹的设备，它将被选择，因为它也将作为覆盖层将二进制文件注入到 `PATH` 中。从 Android 11 开始，`/sbin` 文件夹可能不存在，因此 Magisk 将使用 `/debug_ramdisk` 作为基础文件夹。

```
# 为了获取 Magisk 当前使用的基础文件夹，
# 使用命令 `magisk --path`。
# 像 magisk、magiskinit 和所有指向小程序的符号链接
# 这样的二进制文件直接存储在此路径中。这意味着当
# 这是 /sbin 时，这些二进制文件将直接在 PATH 中。
MAGISKTMP=$(magisk --path)

# Magisk 内部内容
INTERNALDIR=$MAGISKTMP/.magisk

# /data/adb/modules 将被绑定挂载到这里。
# 由于 nosuid 挂载标志，不使用原始文件夹。
$INTERNALDIR/modules

# 当前 Magisk 安装配置
$INTERNALDIR/config

# 分区镜像
# 此路径中的每个目录都将使用其目录名称的分区挂载。
# 例如 system, system_ext, vendor, data ...
$INTERNALDIR/mirror

# 根目录补丁文件
# 在 system-as-root 设备上，/ 不可写。
# 所有预初始化补丁文件都存储在这里并被绑定挂载。
$INTERNALDIR/rootdir
```

### `/data` 中的路径

一些二进制文件和文件应存储在 `/data` 的非易失性存储中。为了防止检测，所有内容都必须存储在 `/data` 中安全且不可检测的地方。选择 `/data/adb` 文件夹是因为以下优势：

- 它是现代 Android 上的现有文件夹，因此不能用作 Magisk 存在的指示。
- 文件夹的权限默认为 `700`，所有者为 `root`，因此非 root 进程无法以任何可能的方式进入、读取、写入该文件夹。
- 该文件夹标记有 secontext `u:object_r:adb_data_file:s0`，很少有进程有权限与该 secontext 进行任何交互。
- 该文件夹位于_设备加密存储_中，因此一旦数据在 FBE（基于文件的加密）设备中正确挂载即可访问。

```
SECURE_DIR=/data/adb

# 存储通用 post-fs-data 脚本的文件夹
$SECURE_DIR/post-fs-data.d

# 存储通用 late_start service 脚本的文件夹
$SECURE_DIR/service.d

# Magisk 模块
$SECURE_DIR/modules

# 等待升级的 Magisk 模块
# 模块文件在挂载时修改不安全
# 通过 Magisk 应用安装的模块将存储在这里
# 并将在下次重启时合并到 $SECURE_DIR/modules 中
$SECURE_DIR/modules_update

# 存储设置和 root 权限的数据库
MAGISKDB=$SECURE_DIR/magisk.db

# 所有 magisk 相关的二进制文件，包括 busybox、
# 脚本和 magisk 二进制文件。用于支持
# 模块安装、addon.d、Magisk 应用等。
DATABIN=$SECURE_DIR/magisk

```

## Magisk 启动过程

### Pre-Init

`magiskinit` 将替换 `init` 作为运行的第一个程序。

- 早期挂载所需的分区。在旧版 system-as-root 设备上，我们切换到 system；在 2SI 设备上，我们修补原始 `init` 以将第 2 阶段 init 文件重定向到 magiskinit 并执行它来为我们挂载分区。
- 将 magisk 服务注入到 `init.rc`
- 在使用单体策略的设备上，从 `/sepolicy` 加载 sepolicy；否则我们使用 FIFO 劫持 selinuxfs 中的节点，设置 `LD_PRELOAD` 来钩住 `security_load_policy` 并在 2SI 设备上协助劫持，并启动守护进程等待 init 尝试加载 sepolicy。
- 修补 sepolicy 规则。如果我们使用"劫持"方法，将修补后的 sepolicy 加载到内核中，解除 init 阻塞并退出守护进程
- 执行原始 `init` 以继续启动过程

### post-fs-data

当 `/data` 被解密和挂载时，这会在 `post-fs-data` 上触发。守护进程 `magiskd` 将被启动，post-fs-data 脚本被执行，模块文件被 magic mount。

### late_start

在启动过程的后期，类 `late_start` 将被触发，Magisk "service" 模式将被启动。在此模式下，service 脚本被执行。

## Resetprop

通常，系统属性被设计为仅由 `init` 更新，对非 root 进程只读。使用 root，你可以通过向 `property_service`（由 `init` 托管）发送请求来更改属性，使用 `setprop` 等命令，但更改只读属性（以 `ro.` 开头的属性，如 `ro.build.product`）和删除属性仍然是被禁止的。

`resetprop` 的实现是从 AOSP 中提取与系统属性相关的源代码并进行修补，以允许直接修改属性区域或 `prop_area`，绕过通过 `property_service` 的需要。由于我们绕过了 `property_service`，有一些注意事项：

- 在 `*.rc` 脚本中注册的 `on property:foo=bar` 操作如果属性更改不通过 `property_service` 将不会被触发。`resetprop` 的默认设置属性行为与 `setprop` 匹配，这**会**触发事件（通过先删除属性然后通过 `property_service` 设置来实现）。有一个标志 `-n` 可以在你需要这种特殊行为时禁用它。
- persist 属性（以 `persist.` 开头的属性，如 `persist.sys.usb.config`）存储在 `prop_area` 和 `/data/property` 中。默认情况下，删除属性**不会**从持久存储中移除它，这意味着属性将在下次重启后恢复；读取属性**不会**从持久存储中读取，因为这是 `getprop` 的行为。使用标志 `-p`，删除属性将**同时**移除 `prop_area` 和 `/data/property` 中的属性，读取属性将从 `prop_area` 和持久存储**两者**中读取。

## SELinux 策略

Magisk 将修补原始 `sepolicy` 以确保 root 和 Magisk 操作可以安全可靠地完成。新域 `magisk` 实际上是宽容的，`magiskd` 和所有 root shell 将在其中运行。`magisk_file` 是一个新文件类型，设置为允许被每个域访问（不受限制的文件上下文）。

在 Android 8.0 之前，所有允许的 su 客户端域都可以直接连接到 `magiskd` 并与守护进程建立连接以获取远程 root shell。Magisk 还必须放宽一些 `ioctl` 操作，以便 root shell 可以正常运行。

在 Android 8.0 之后，为了减少 Android 沙箱中规则的放宽，部署了一个新的 SELinux 模型。`magisk` 二进制文件标记有 `magisk_exec` 文件类型，作为允许的 su 客户端域运行的进程执行 `magisk` 二进制文件（这包括 `su` 命令）将通过使用 `type_transition` 规则转变为 `magisk_client`。规则严格限制只有 `magisk` 域进程才允许将文件归因于 `magisk_exec`。不允许直接连接到 `magiskd` 的套接字；访问守护进程的唯一方式是通过 `magisk_client` 进程。这些更改允许我们保持沙箱完整，并将 Magisk 特定规则与策略的其余部分分离。

完整的规则集可以在 `sepolicy/rules.cpp` 中找到。
