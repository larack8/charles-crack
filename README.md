# charles-crack

[中文](README_zh.md) | **English**

Open-source Charles Proxy license keygen, implemented from scratch with the RC5 block cipher. Compatible with every historical Charles 3.x / 4.x / 5.x release.

This repo ships a small, dependency-free Java codebase plus a build script that produces:

- A cross-platform **runnable JAR** (`charles-keygen.jar`) — works anywhere a JRE 11+ is installed.
- A **native desktop app / installer** for the host OS via `jpackage`:
  - **macOS** → `CharlesKeygen.app` bundle + `.dmg` installer
  - **Windows** → `CharlesKeygen.exe` launcher + `.msi` installer
  - **Linux** → `CharlesKeygen` binary + `.deb` package (and `.rpm` on rpm-based distros)

> ⚠️ For educational and research purposes only. Please purchase a legitimate Charles license at <https://www.charlesproxy.com/> if you use it for work.

---

## Project layout

```
charles-crack/
├── src/
│   ├── Main.java            # Cross-platform entry point: GUI + CLI + --help
│   ├── CharlesKeygen.java   # Key derivation logic (license name → license key)
│   └── RC5.java             # Pure-Java RC5-32/12/16 implementation
├── build.sh                 # Build script for macOS / Linux (bash)
├── build.bat                # Build script for Windows (cmd)
├── build/                   # Intermediate output (classes, MANIFEST, fat JAR)
└── dist/                    # Final artifacts, per OS (jar, .app, .dmg, .exe, .msi, .deb, ...)
```

## Requirements

- **JDK 17 or newer** is recommended (required for `jpackage` native bundling).
  - JDK 11+ is sufficient if you only want the runnable JAR.
- Platform-specific bundling tools used by `jpackage`:
  - **macOS**: Xcode Command Line Tools (`xcode-select --install`)
  - **Windows**: [WiX Toolset 3.x](https://wixtoolset.org/releases/) on `PATH` (for `.msi`)
  - **Linux**: `fakeroot` + `dpkg` (for `.deb`) or `rpmbuild` (for `.rpm`)

Verify your toolchain:

```bash
java  -version
javac -version
jpackage --version
```

## Build

### macOS / Linux

```bash
./build.sh
```

### Windows

```cmd
build.bat
```

Both scripts perform the same pipeline:

1. Compile `src/*.java` → `build/classes/`
2. Package a runnable JAR → `build/charles-keygen.jar` (manifest sets `Main-Class: Main`)
3. Invoke `jpackage` to produce native artifacts under `dist/<os>/`

Example output on macOS:

```
build/
└── charles-keygen.jar
dist/
└── macos/
    ├── CharlesKeygen.app
    └── CharlesKeygen-1.0.0.dmg
```

## Usage

### 1. GUI (double-click / launch app)

Run the native bundle (`CharlesKeygen.app` / `CharlesKeygen.exe` / `./CharlesKeygen`) or:

```bash
java -jar build/charles-keygen.jar
```

A small Swing window opens. Enter any license name, press **Generate**, and the key is filled in and ready to copy.

### 2. Command line

```bash
# Shortcut: single positional arg
java -jar build/charles-keygen.jar "Your Name"

# Explicit CLI flag
java -jar build/charles-keygen.jar --cli "Your Name"

# Help
java -jar build/charles-keygen.jar --help
```

Sample output:

```
Charles Proxy - License Key Generator [ALL VERSIONS]
* GENERATED LICENSE:
  Name: Your Name
  Key:  44E7A0EAFB50CCD696
```

## Applying the license to Charles

1. Open **Charles → Help → Register Charles…**
2. Paste the **Name** and **Key** exactly as generated.
3. Restart Charles.

If Charles refuses to launch on newer macOS versions due to license verification, also add the official fake-license patch (see the Charles docs) — this keygen generates keys that pass the local RC5 check, but network-based verification is out of scope.

## How it works (brief)

Charles encodes the license key as:

```
Key = RC5-encrypt( f(name) , staticKey ) formatted as hex
```

where:

- `f(name)` derives a 64-bit block from the license name using a small custom mix.
- `staticKey` is the well-known 128-bit RC5 key embedded in every Charles build since 3.x.

`RC5.java` implements RC5-32/12/16 (32-bit words, 12 rounds, 16-byte key), and `CharlesKeygen.java` wires it together. See the source for the exact algorithm.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `jpackage: command not found` | Install JDK 17+ and make sure `$JAVA_HOME/bin` is on `PATH`. |
| `jpackage` fails on Windows with WiX error | Install WiX 3.x and add its `bin/` to `PATH`, or build only the `app-image` type. |
| `jpackage` fails on Linux with "Cannot find dpkg" | Install `fakeroot dpkg` (Debian/Ubuntu) or `rpm-build` (Fedora/RHEL). |
| GUI doesn't open on Linux over SSH | You need an X11/Wayland display; use the CLI mode instead. |
| `UnsupportedClassVersionError` when running the JAR | Upgrade your JRE to 11+ (17+ recommended). |

## License

Source code is released under the MIT License — see [LICENSE](LICENSE).

"Charles" and "Charles Proxy" are trademarks of XK72 Ltd. This project is not affiliated with, endorsed by, or sponsored by XK72 Ltd.
