# 版本历史

## v1.0.0

首个版本。

- 纯 Java（JDK 17+）实现的 Charles Proxy 许可证生成器，无第三方依赖。
- 统一入口 `Main.java`：默认 Swing GUI，无显示环境自动降级 CLI，也可用 `--cli` 强制 CLI。
- `build.sh` / `build.bat`（javac + jpackage）一键生成全平台产物：
    - `CharlesKeygen-1.0.0.jar`
    - macOS：`.dmg` + `-app.zip`
    - Windows：`.exe` / `.zip` / `.msi`
    - Linux：`.tar.gz`
- 由 `assets/logo.svg` 生成图标：`.icns` / `.ico` / `.png`。
- 双语文档：`README.md` + `README_zh.md`。
