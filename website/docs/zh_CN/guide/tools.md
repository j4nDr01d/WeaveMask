# Magisk 工具

Magisk 附带了大量的安装、守护进程和开发者实用工具。本文档涵盖 4 个二进制文件和所有包含的小程序。二进制文件和小程序如下所示：

```
magiskboot                 /* 二进制文件 */
magiskinit                 /* 二进制文件 */
magiskpolicy               /* 二进制文件 */
supolicy -> magiskpolicy
magisk                     /* 二进制文件 */
resetprop -> magisk
su -> magisk
```

### magiskboot

一个用于解包/重新打包 boot 镜像、解析/修补/提取 cpio、修补 dtb、十六进制修补二进制文件以及使用多种算法压缩/解压缩文件的工具。

`magiskboot` 原生支持（意味着它不依赖外部工具）常见的压缩格式，包括 `gzip`、`lz4`、`lz4_legacy`、`lz4_lg`（[LG 版本](https://events.static.linuxfound.org/sites/events/files/lcjpcojp13_klee.pdf)的 `lz4_legacy`，仅在 LG 上使用）、`lzma`、`xz` 和 `bzip2`。

`magiskboot` 的概念是使 boot 镜像修改更简单。对于解包，它解析头文件并提取镜像中的所有部分，如果检测到任何部分的压缩，则即时解压缩。对于重新打包，需要原始 boot 镜像，以便可以使用原始头文件，仅更改必要的条目，如部分大小和校验和。如果需要，所有部分将被压缩回原始格式。该工具还支持许多 CPIO 和 DTB 操作。

```
用法：./magiskboot <action> [args...]

支持的操作：
  unpack [-n] [-h] <bootimg>
    将 <bootimg> 解包到其各个组件，每个组件输出到
    当前目录中具有相应文件名的文件。
    支持的组件：kernel, kernel_dtb, ramdisk.cpio, second,
    dtb, extra, 和 recovery_dtbo。
    默认情况下，每个组件将在写入输出文件之前
    自动即时解压缩。
    如果提供 '-n'，所有解压缩操作将被跳过；
    每个组件将保持不变，以其原始格式转储。
    如果提供 '-h'，boot 镜像头信息将被
    转储到文件 'header'，可用于在重新打包期间
    修改头配置。
    返回值：
    0:有效    1:错误    2:chromeos

  repack [-n] <origbootimg> [outbootimg]
    使用当前目录中的文件将 boot 镜像组件重新打包
    到 [outbootimg]，如果未指定则为 'new-boot.img'。当前目录
    应仅包含 [outbootimg] 所需的文件，否则可能产生
    不正确的 [outbootimg]。
    <origbootimg> 是用于解包组件的原始 boot 镜像。
    默认情况下，每个组件将使用在 <origbootimg> 中检测到的
    相应格式自动压缩。如果当前目录中的组件文件
    已经压缩，则不会对该特定组件执行额外压缩。
    如果提供 '-n'，所有压缩操作将被跳过。
    如果环境变量 PATCHVBMETAFLAG 设置为 true，boot 镜像的
    vbmeta 头中的所有禁用标志将被设置。

  verify <bootimg> [x509.pem]
    检查 boot 镜像是否使用 AVB 1.0 签名签名。
    可选提供证书以验证镜像是否由公钥证书签名。
    返回值：
    0:有效    1:错误

  sign <bootimg> [name] [x509.pem pk8]
    使用 AVB 1.0 签名对 <bootimg> 进行签名。
    可选提供镜像名称（默认：'/boot'）。
    可选提供证书/私钥对进行签名。
    如果未提供证书/私钥对，将使用可执行文件中捆绑的
    AOSP verity 密钥。

  extract <payload.bin> [partition] [outfile]
    从 <payload.bin> 提取 [partition] 到 [outfile]。
    如果未指定 [outfile]，则输出到 '[partition].img'。
    如果未指定 [partition]，则尝试提取 'init_boot' 或 'boot'。
    可以通过 'init_boot.img' 或 'boot.img' 哪个存在来确定
    选择了哪个分区。
    <payload.bin> 可以是 '-' 表示标准输入。

  hexpatch <file> <hexpattern1> <hexpattern2>
    在 <file> 中搜索 <hexpattern1>，并用 <hexpattern2> 替换它

  cpio <incpio> [commands...]
    对 <incpio> 执行 cpio 命令（修改在原地进行）
    每个命令是单个参数，为每个命令添加引号。
    支持的命令：
      exists ENTRY
        如果 ENTRY 存在则返回 0，否则返回 1
      rm [-r] ENTRY
        移除 ENTRY，指定 [-r] 递归移除
      mkdir MODE ENTRY
        在权限 MODE 下在 ENTRY 创建目录
      ln TARGET ENTRY
        创建指向 TARGET 的符号链接，名称为 ENTRY
      mv SOURCE DEST
        将 SOURCE 移动到 DEST
      add MODE ENTRY INFILE
        将 INFILE 作为 ENTRY 添加到权限 MODE；如果存在则替换 ENTRY
      extract [ENTRY OUT]
        将 ENTRY 提取到 OUT，或提取所有条目到当前目录
      test
        测试 cpio 的状态
        返回值是 0 或以下值的按位或：
        0x1:Magisk    0x2:不支持
      patch
        应用 ramdisk 补丁
        使用环境变量配置：KEEPVERITY KEEPFORCEENCRYPT
      backup ORIG
        从 ORIG 创建 ramdisk 备份
      restore
        从 incpio 中存储的 ramdisk 备份恢复 ramdisk

  dtb <file> <action> [args...]
    对 <file> 执行 dtb 相关操作
    支持的操作：
      print [-f]
        打印 dtb 的所有内容用于调试
        指定 [-f] 仅打印 fstab 节点
      patch
        搜索 fstab 并移除 verity/avb
        修改直接在文件中进行
        使用环境变量配置：KEEPVERITY
      test
        测试 fstab 的状态
        返回值：
        0:有效    1:错误

  split <file>
    将 image.*-dtb 分割为 kernel + kernel_dtb

  sha1 <file>
    打印 <file> 的 SHA1 校验和

  cleanup
    清理当前工作目录

  compress[=format] <infile> [outfile]
    使用 [format] 将 <infile> 压缩到 [outfile]。
    <infile>/[outfile] 可以是 '-' 表示标准输入/标准输出。
    如果未指定 [format]，则使用 gzip。
    如果未指定 [outfile]，则 <infile> 将被替换为
    带有匹配文件扩展名的另一个文件。
    支持的格式：gzip zopfli xz lzma bzip2 lz4 lz4_legacy lz4_lg

  decompress <infile> [outfile]
    检测格式并将 <infile> 解压缩到 [outfile]。
    <infile>/[outfile] 可以是 '-' 表示标准输入/标准输出。
    如果未指定 [outfile]，则 <infile> 将被替换为
    移除其归档格式文件扩展名的另一个文件。
    支持的格式：gzip zopfli xz lzma bzip2 lz4 lz4_legacy lz4_lg
```

### magiskinit

此二进制文件将替换 Magisk 修补的 boot 镜像 ramdisk 中的 `init`。它最初是为支持使用 system-as-root 的设备而创建的，但该工具已扩展为支持所有设备，并成为 Magisk 的关键部分。更多详细信息可以在 [Magisk 启动过程](/zh_CN/guide/details#magisk-启动过程)的 **Pre-Init** 部分找到。

### magiskpolicy

（此工具别名为 `supolicy`，以兼容 SuperSU 的 sepolicy 工具）

此工具可用于高级开发者修改 SELinux 策略。在常见场景中，如 Linux 服务器管理员，他们会直接修改 SELinux 策略源（`*.te`）并重新编译 `sepolicy` 二进制文件，但在这里在 Android 上，我们直接修补二进制文件（或运行时策略）。

从 Magisk 守护进程生成的所有进程，包括 root shell 及其所有分支，都在上下文 `u:r:magisk:s0` 中运行。在所有 Magisk 安装系统上使用的规则可以看作是带有这些补丁的原始 `sepolicy`：`magiskpolicy --magisk 'allow magisk * * *'`。

```
用法：./magiskpolicy [--options...] [policy statements...]

选项：
   --help            显示策略语句的帮助消息
   --load FILE       从 FILE 加载单体 sepolicy
   --load-split      从预编译的 sepolicy 加载或编译
                     拆分 cil 策略
   --compile-split   编译拆分 cil 策略
   --save FILE       将单体 sepolicy 转储到 FILE
   --live            立即将 sepolicy 加载到内核
   --magisk          应用内置的 Magisk sepolicy 规则
   --apply FILE      从 FILE 应用规则，逐行读取和解析
                     作为策略语句
                     （允许多个 --apply）

如果既未指定 --load、--load-split 也未指定 --compile-split，
它将从当前活动策略加载（/sys/fs/selinux/policy）

一个策略语句应被视为一个参数；
这意味着每个策略语句都应括在引号中。
可以在单个命令中提供多个策略语句。

语句格式为 "<rule_name> [args...]"。
标记为 (^) 的参数可以接受一个或多个条目。多个
条目由括在大括号 ({}) 中的空格分隔列表组成。
标记为 (*) 的参数与 (^) 相同，但额外
支持匹配所有运算符 (*)。

示例："allow { s1 s2 } { t1 t2 } class *"
将扩展为：

allow s1 t1 class { all-permissions-of-class }
allow s1 t2 class { all-permissions-of-class }
allow s2 t1 class { all-permissions-of-class }
allow s2 t2 class { all-permissions-of-class }

支持的策略语句：

"allow *source_type *target_type *class *perm_set"
"deny *source_type *target_type *class *perm_set"
"auditallow *source_type *target_type *class *perm_set"
"dontaudit *source_type *target_type *class *perm_set"

"allowxperm *source_type *target_type *class operation xperm_set"
"auditallowxperm *source_type *target_type *class operation xperm_set"
"dontauditxperm *source_type *target_type *class operation xperm_set"
- 唯一支持的操作是 'ioctl'
- xperm_set 格式为 'low-high'、'value' 或 '*'。
  '*' 将被视为 '0x0000-0xFFFF'。
  所有值应以十六进制书写。

"permissive ^type"
"enforce ^type"

"typeattribute ^type ^attribute"

"type type_name ^(attribute)"
- 参数 'attribute' 是可选的，默认为 'domain'

"attribute attribute_name"

"type_transition source_type target_type class default_type (object_name)"
- 参数 'object_name' 是可选的

"type_change source_type target_type class default_type"
"type_member source_type target_type class default_type"

"genfscon fs_name partial_path fs_context"
```

### magisk

当 magisk 二进制文件以名称 `magisk` 调用时，它作为实用工具工作，具有许多辅助功能和多个 Magisk 服务的入口点。

```
用法：magisk [applet [arguments]...]
   或：magisk [options]...

选项：
   -c                        打印当前二进制版本
   -v                        打印运行中的守护进程版本
   -V                        打印运行中的守护进程版本代码
   --list                    列出所有可用的小程序
   --remove-modules [-n]     移除所有模块，如果未提供 -n 则重启
   --install-module ZIP      安装模块 zip 文件

高级选项（内部 API）：
   --daemon                  手动启动 magisk 守护进程
   --stop                    移除所有 magisk 更改并停止守护进程
   --[init trigger]          在 init 触发器上的回调。有效触发器：
                             post-fs-data, service, boot-complete, zygote-restart
   --unlock-blocks           将所有块设备的 BLKROSET 标志设置为 OFF
   --restorecon              恢复 Magisk 文件上的 selinux 上下文
   --clone-attr SRC DEST     克隆权限、所有者和 selinux 上下文
   --clone SRC DEST          将 SRC 克隆到 DEST
   --sqlite SQL              对 Magisk 数据库执行 SQL 命令
   --path                    打印 Magisk tmpfs 挂载路径
   --denylist ARGS           denylist 配置 CLI
   --preinit-device          解析设备以存储 preinit 文件

可用小程序：
    su, resetprop

用法：magisk --denylist [action [arguments...] ]
操作：
   status          返回执行状态
   enable          启用 denylist 执行
   disable         禁用 denylist 执行
   add PKG [PROC]  添加新的目标到 denylist
   rm PKG [PROC]   从 denylist 移除目标
   ls              打印当前 denylist
   exec CMDs...    在隔离的挂载命名空间中执行命令
                   并执行所有卸载
```

### su

`magisk` 的小程序，MagiskSU 入口点。经典的 `su` 命令。

```
用法：su [options] [-] [user [argument...]]

选项：
  -c, --command COMMAND         将 COMMAND 传递给调用的 shell
  -g, --group GROUP             指定主组
  -G, --supp-group GROUP        指定补充组。
                                如果未指定 -g 选项，第一个指定的补充组也用作主组。
  -Z, --context CONTEXT         更改 SELinux 上下文
  -t, --target PID              从中获取挂载命名空间的 PID
  -h, --help                    显示此帮助消息并退出
  -, -l, --login                假装 shell 是登录 shell
  -m, -p,
  --preserve-environment        保留整个环境
  -s, --shell SHELL             使用 SHELL 代替默认的 /system/bin/sh
  -v, --version                 显示版本号并退出
  -V                            显示版本代码并退出
  -mm, -M,
  --mount-master                强制在全局挂载命名空间中运行
```

### resetprop

`magisk` 的小程序。一个高级系统属性操作实用工具。查看 [Resetprop 详细信息](/zh_CN/guide/details#resetprop) 了解更多背景信息。

```
用法：resetprop [flags] [options...]

选项：
   -h, --help        显示此消息
   （无参数）         打印所有属性
   NAME              获取属性
   NAME VALUE        设置属性条目 NAME 的值为 VALUE
   --file FILE       从 FILE 加载属性
   --delete NAME     删除属性

标志：
   -v      将详细输出打印到标准错误
   -n      设置属性时不通过 property_service
           （此标志仅影响 setprop）
   -p      从/到持久存储读取/写入属性
           （此标志仅影响 getprop 和 delprop）
```
