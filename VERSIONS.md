# Versions

## v1.0.0

Initial release.

- Pure Java (JDK 17+) Charles Proxy license key generator, no third-party deps.
- Unified `Main.java`: Swing GUI by default, auto-fallback to CLI on headless, or `--cli` to force.
- Cross-platform artifacts via `build.sh` / `build.bat` (javac + jpackage):
    - `CharlesKeygen-1.0.0.jar`
    - macOS: `.dmg` + `-app.zip`
    - Windows: `.exe` / `.zip` / `.msi`
    - Linux: `.tar.gz`
- Icons generated from `assets/logo.svg` → `.icns` / `.ico` / `.png`.
- Bilingual docs: `README.md` + `README_zh.md`.
