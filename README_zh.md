<p align="center">
  <img src="assets/logo.svg" alt="CharlesKeygen logo" width="160" height="160"/>
</p>

<h1 align="center">charles-crack</h1>

<p align="center"><b>中文</b> | <a href="README.md">English</a></p>

开源的 Charles Proxy 许可证注册机，基于 RC5 分组密码算法从零实现，兼容历史上所有 Charles 3.x / 4.x / 5.x 版本。

本仓库包含一套零依赖的 Java 源码，以及一键构建脚本，可产出如下产物：

- 跨平台的**可执行 JAR**，在安装了 JRE 11+ 的任何系统上均可运行。
- 通过 `jpackage` 打包的**原生桌面应用 / 安装包**：
  - **macOS** → `.dmg` 安装镜像 + `.app` 应用打包 zip
  - **Windows** → `.exe` 启动器 + `.msi` 安装包 + 自包含 zip
  - **Linux** → 自包含的 `.tar.gz` 运行目录

所有最终产物都会被移动到 [`release/`](release) 目录，遵循统一命名规范（见下）。

> ⚠️ 仅用于学习与技术研究。如在工作环境中使用，请在 <https://www.charlesproxy.com/> 购买正版授权。

---

## 项目结构

```
charles-crack/
├── assets/
│   ├── logo.svg             # 矢量 Logo（唯一真源）
│   ├── logo.png             # 512×512 位图（Linux 图标）
│   ├── logo.icns            # macOS 图标包
│   ├── logo.ico             # Windows 多分辨率图标
│   └── logo-{16..1024}.png  # 各分辨率 PNG
├── src/
│   ├── Main.java            # 跨平台入口：GUI + 命令行 + --help
│   ├── CharlesKeygen.java   # 密钥派生逻辑（name → key）
│   └── RC5.java             # 纯 Java 实现的 RC5-32/12/16
├── build.sh                 # macOS / Linux 构建脚本（bash）
├── build.bat                # Windows 构建脚本（cmd）
├── build/                   # 中间产物（字节码、MANIFEST、Fat JAR）
├── dist/                    # jpackage 原始输出（按 OS 分目录）
└── release/                 # 经过重命名的最终发布产物
```

## 发布产物与命名

所有最终二进制都会被放到 [`release/`](release)，命名规范：

```
CharlesKeygen-<version>[-<os>-<arch>][-<variant>].<ext>
```

| 平台 | 文件 |
|---|---|
| **通用** | `CharlesKeygen-1.0.0.jar` |
| **macOS（Apple Silicon）** | `CharlesKeygen-1.0.0-macos-arm64.dmg`<br>`CharlesKeygen-1.0.0-macos-arm64-app.zip` |
| **macOS（Intel）** | `CharlesKeygen-1.0.0-macos-x64.dmg`<br>`CharlesKeygen-1.0.0-macos-x64-app.zip` |
| **Windows x64** | `CharlesKeygen-1.0.0-windows-x64.exe`<br>`CharlesKeygen-1.0.0-windows-x64.zip`<br>`CharlesKeygen-1.0.0-windows-x64.msi` |
| **Linux x64** | `CharlesKeygen-1.0.0-linux-x64.tar.gz` |
| **Linux arm64** | `CharlesKeygen-1.0.0-linux-arm64.tar.gz` |

`jpackage` 只能为当前运行 OS 构建原生安装包——想要全部平台的产物，需在对应 OS 上分别运行 `build.sh` / `build.bat`（或接入 CI）。`.jar` 则在每次构建时都会产出，天生跨平台。

## 环境要求

- **推荐 JDK 17 及以上**（`jpackage` 原生打包所需）。
  - 如果只想要跨平台 JAR，JDK 11+ 即可。
- `jpackage` 依赖的平台工具链：
  - **macOS**：Xcode Command Line Tools（`xcode-select --install`）
  - **Windows**：[WiX Toolset 3.x](https://wixtoolset.org/releases/)，并加入 `PATH`（用于生成 `.msi`）
  - **Linux**：`fakeroot` + `dpkg`（生成 `.deb`）或 `rpmbuild`（生成 `.rpm`）

检查工具链：

```bash
java  -version
javac -version
jpackage --version
```

## 构建

### macOS / Linux

```bash
./build.sh
```

### Windows

```cmd
build.bat
```

两个脚本执行完全相同的流水线：

1. 编译 `src/*.java` → `build/classes/`
2. 打包为可执行 JAR → `build/charles-keygen.jar`（MANIFEST 中设置 `Main-Class: Main`）
3. 使用 `assets/` 中的平台图标调用 `jpackage`，在 `dist/<os>/` 下生成原生产物
4. **将所有产物按上述命名规范复制/重命名到 `release/` 目录**

在 Apple Silicon macOS 上的典型输出：

```
release/
├── CharlesKeygen-1.0.0.jar
├── CharlesKeygen-1.0.0-macos-arm64.dmg
└── CharlesKeygen-1.0.0-macos-arm64-app.zip
```

## 使用方式

### 1. 图形界面（双击运行）

使用对应平台的原生包（`.dmg` / `.msi` / `.tar.gz`）安装并启动；或直接运行 JAR：

```bash
java -jar release/CharlesKeygen-1.0.0.jar
```

弹出一个简洁的 Swing 窗口：输入任意 License Name，点击 **Generate**，即可得到对应的 Key，可一键复制。

### 2. 命令行

```bash
# 简写形式：直接传名字
java -jar release/CharlesKeygen-1.0.0.jar "Your Name"

# 显式 CLI 模式
java -jar release/CharlesKeygen-1.0.0.jar --cli "Your Name"

# 帮助
java -jar release/CharlesKeygen-1.0.0.jar --help
```

输出示例：

```
Charles Proxy - License Key Generator [ALL VERSIONS]
* GENERATED LICENSE:
  Name: Your Name
  Key:  44E7A0EAFB50CCD696
```

## 在 Charles 中激活

1. 打开 **Charles → Help → Register Charles…**
2. 将生成的 **Name** 与 **Key** 完整粘贴进去（注意不要有多余空格）。
3. 重启 Charles。

如果新版 macOS 因为联网校验导致 Charles 无法启动，可以结合社区流传的离线补丁使用——本注册机只负责通过 **本地 RC5 校验**，联网校验不在本项目范围内。

## 算法简介

Charles 的注册码生成可以简写为：

```
Key = RC5-encrypt( f(name), staticKey )，并以十六进制字符串输出
```

其中：

- `f(name)`：对 License Name 做一次自定义混合，得到一个 64-bit 明文分组。
- `staticKey`：Charles 自 3.x 起嵌在程序中的 128-bit RC5 密钥，网上已广为人知。

`RC5.java` 是一份纯 Java 实现的 RC5-32/12/16（32 位字长、12 轮、16 字节密钥），`CharlesKeygen.java` 将这两者串起来。具体算法细节请直接阅读源码。

## 重新生成 Logo / 图标

Logo 源文件位于 `assets/logo.svg`。若修改了该 SVG，按以下步骤重新生成各平台图标：

```bash
cd assets
for s in 16 32 64 128 256 512 1024; do
  rsvg-convert -w $s -h $s logo.svg -o logo-${s}.png
done

# macOS .icns
mkdir -p logo.iconset
cp logo-16.png   logo.iconset/icon_16x16.png
cp logo-32.png   logo.iconset/icon_16x16@2x.png
cp logo-32.png   logo.iconset/icon_32x32.png
cp logo-64.png   logo.iconset/icon_32x32@2x.png
cp logo-128.png  logo.iconset/icon_128x128.png
cp logo-256.png  logo.iconset/icon_128x128@2x.png
cp logo-256.png  logo.iconset/icon_256x256.png
cp logo-512.png  logo.iconset/icon_256x256@2x.png
cp logo-512.png  logo.iconset/icon_512x512.png
cp logo-1024.png logo.iconset/icon_512x512@2x.png
iconutil -c icns logo.iconset -o logo.icns
rm -rf logo.iconset

# Windows .ico（需要 ImageMagick）
magick logo-16.png logo-32.png logo-64.png logo-128.png logo-256.png logo.ico

# Linux PNG
cp logo-512.png logo.png
```

## 常见问题

| 现象 | 解决方法 |
|---|---|
| 提示 `jpackage: command not found` | 安装 JDK 17+，并把 `$JAVA_HOME/bin` 加入 `PATH`。 |
| Windows 上 `jpackage` 报 WiX 错误 | 安装 WiX 3.x 并把 `bin/` 加入 `PATH`；或仅构建 `app-image` 类型。 |
| Linux 上 `jpackage` 提示找不到 `dpkg` | Debian/Ubuntu 安装 `fakeroot dpkg`；Fedora/RHEL 安装 `rpm-build`。 |
| 通过 SSH 无法打开图形界面 | 远程环境没有 X11/Wayland 显示；请改用命令行模式。 |
| 运行 JAR 报 `UnsupportedClassVersionError` | 升级 JRE 到 11+（推荐 17+）。 |

## 许可协议

源代码以 MIT 协议发布，详见 [LICENSE](LICENSE)。

"Charles" 与 "Charles Proxy" 是 XK72 Ltd 的商标，本项目与其不存在任何从属或合作关系。
