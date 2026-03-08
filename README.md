# AndroHunter

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin"/>
  <img src="https://img.shields.io/badge/Min_SDK-29_(Android_10)-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Version-4.0-00FF88?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge"/>
</p>

<p align="center">
  <b>A comprehensive Android security research toolkit for bug bounty hunters and mobile penetration testers.</b><br/>
  Built with Jetpack Compose · Dark terminal UI · On-device analysis
</p>

---

## ⚠️ Legal Disclaimer

> **AndroHunter is intended for authorized security research, bug bounty programs, and educational purposes only.**
> You must have explicit permission from the application owner before testing any target.
> The developer assumes no responsibility for misuse. Always comply with your bug bounty program's scope and rules of engagement.

---

## Overview

AndroHunter is a native Android application that provides a full suite of mobile security testing tools — all running directly on the device without requiring a rooted phone for most features. It is designed for security researchers participating in bug bounty programs (HackerOne, Yes We Hack, Intigriti, etc.) who need to analyze Android applications quickly and efficiently.

The tool covers the entire Android attack surface: static analysis (APK, DEX, Manifest), dynamic testing (Intent fuzzing, ContentProvider probing, Broadcast injection), runtime analysis (Frida script generation, SSL bypass), and network interception (HTTP proxy).

---

## Features

### 📱 App Explorer
- Lists all installed applications with metadata (package name, version, permissions, target SDK)
- Filter by system/user apps
- Quick navigation to any analysis module from the app detail view

### 🔍 DEX Analyzer
- Extracts and analyzes `.dex` files from APKs
- Scans for hardcoded secrets: API keys, tokens, passwords, URLs, private keys
- String pattern matching with severity classification (`VULN` / `SUSP` / `SAFE`)
- Class and method enumeration with popup viewer
- Supports multi-dex APKs — each DEX file analyzed separately

### 📄 Manifest Viewer
- Parses `AndroidManifest.xml` directly from the APK (no decompiler needed)
- Three-tab view: **Components**, **Permissions**, **Raw XML**
- Highlights exported components, dangerous permissions, and deep link schemes
- Identifies potential attack surface (exported Activities, Services, Receivers, Providers)

### 🎯 Intent Fuzzer
- Lists all exported Activities, Services, and Broadcast Receivers of the target app
- Sends crafted Intents with custom extras, data URIs, and categories
- Supports path traversal payloads via Intent data (`file:///data/...`)
- Integrates with Payload Engine for automated testing

### 💥 Payload Engine
- Logcat-based real-time result monitoring
- Automated payload delivery to target components
- Visual result classification: `VULN` (red) / `SUSP` (yellow) / `SAFE` (green)
- Supports deeplink exploitation, OAuth redirect hijacking, file URI leaks

### 🗄️ Content Provider Fuzzer
- Enumerates all exported ContentProviders of the target application
- Tests 9 SQL injection payloads per provider (Error-based, Boolean, UNION, Time-based)
- Detects readable/writable providers and schema exposure
- One-tap navigation from APK Analyzer findings to Provider Fuzzer with pre-filled target

### 📁 FileProvider Path Analyzer
- Parses `res/xml/` configuration files from APK to extract FileProvider path definitions
- Risk classification per path type:
  - `root-path` with empty path → **CRITICAL** (full filesystem access)
  - `external-path` with empty path → **HIGH**
  - `cache-path` / `external-cache-path` → **MEDIUM**
- **Path Traversal Tester**: automated testing with 9 traversal payloads
- Attempts actual file reads via `ContentResolver` and reports file contents on success
- **ADB Commands tab**: ready-to-use `adb shell content read --uri '...'` commands

### 🏃 Activity Launcher
- Lists all Activities of any installed app with exported status badge
- One-tap launch with optional extra data / deep link injection
- ADB command generator: `adb shell am start -n pkg/activity --es data "payload"`
- Filter by exported-only for quick attack surface identification

### 📡 Broadcast Fuzzer
- 10 pre-built broadcast payloads across 6 categories:
  - **Auth**: Login bypass, Session hijack
  - **SQLi**: SQL injection via Intent extras
  - **LFI**: Path traversal via file path extras
  - **Redirect**: Open redirect, Deep link hijack
  - **PrivEsc**: Privilege escalation, Component enable
  - **Exfil**: Data exfiltration via backup intent
- Custom broadcast sender: specify action + key=value extras
- ADB command copy for each payload

### 🔑 Shared Preferences Reader
- Reads `shared_prefs/*.xml` files from target application data directory
- Uses `run-as` for debug apps, falls back to `dumpsys` for others
- Sensitive key detection: `token`, `password`, `secret`, `api_key`, `session`, `jwt`, `cookie`
- Filter by sensitive-only, full text search, one-tap copy

### 🐛 Frida Script Generator
- Generates ready-to-use Frida hook scripts tailored to the selected target package
- 6 script categories:
  - **SSL Pinning Bypass**: OkHttp3, TrustManager, Conscrypt, BoringSSL
  - **Root Detection Bypass**: RootBeer, SafetyNet, `File.exists()` hook
  - **Login Bypass**: Auto-discovers auth/login/session classes via reflection
  - **Crypto Monitor**: Hooks `javax.crypto.Cipher` — logs all encrypt/decrypt operations
  - **SQL Monitor**: Hooks `SQLiteDatabase.rawQuery`, `execSQL`, `query`
  - **HTTP Intercept**: Hooks OkHttp3 and `HttpURLConnection`
- One-tap copy with or without launch command header
- Ready-to-run command: `frida -U -f com.target.app -l script.js --no-pause`

### 🔓 SSL Pinning Bypass Guide
- 6 bypass methods with step-by-step instructions:
  1. **Frida SSL Kill Switch 2** — easiest, no root needed
  2. **objection** — `android sslpinning disable`
  3. **Magisk TrustMeAlready** — system-wide bypass
  4. **APK Repack** — inject `network_security_config.xml` via `apktool`
  5. **Xposed / LSPosed + JustTrustMe**
  6. **Burp Suite + User CA**

### 🌐 Traffic Interceptor
- Built-in HTTP proxy server running on `127.0.0.1:8877`
- Captures HTTP traffic from any application configured to use the proxy
- HTTPS CONNECT tunnel support
- Real-time request/response list with method color coding
- Sensitive header highlighting: `Authorization`, `Cookie`, `Token` shown in red
- Per-request detail view: full headers, request body, response body, timing
- **curl command generator**: one-tap copy of any captured request
- Filter by URL, host, body content, or HTTP method

### 🖥️ Terminal
- On-device shell command execution
- Quick command chips: `id`, `whoami`, `uname -a`, `env`, `ifconfig`, `netstat -an`, `ps`, `ls /data`
- Color-coded output: commands (green), stdout (white), stderr (red)
- IME padding: input bar stays visible when keyboard opens

### 👁️ Broadcast Monitor
- Live monitor for system and custom broadcast intents

### 🎭 Task Hijack (StrandHogg)
- Tests for Task Affinity hijacking vulnerability (StrandHogg 1.0)

### ♿ Accessibility Monitor
- Monitors Accessibility Service events from target applications

---

## Module Architecture
```
com.androhunter.app/
├── MainActivity.kt
├── AndroHunterApp.kt
├── navigation/
│   ├── Screen.kt                    # Type-safe @Serializable destinations
│   └── NavGraph.kt                  # Compose NavHost routing
├── core/
│   ├── Strings.kt                   # Pattern matching constants
│   └── Language.kt
├── ui/
│   ├── applist/       AppListScreen
│   ├── appdetail/     AppDetailScreen
│   ├── dex/           DexAnalyzerScreen
│   ├── manifest/      ManifestViewerScreen
│   ├── intent/        IntentFuzzerScreen
│   ├── payload/       PayloadEngine + PayloadScreen
│   ├── providers/     ContentProviderScreen
│   ├── fileprovider/  FileProviderScreen
│   ├── actlauncher/   ActivityLauncherScreen
│   ├── broadcast/     BroadcastFuzzerScreen
│   ├── prefs/         SharedPrefsScreen
│   ├── frida/         FridaGeneratorScreen
│   ├── ssl/           SslBypassScreen
│   ├── traffic/
│   │   ├── ProxyServer.kt           # Raw ServerSocket proxy on :8877
│   │   └── TrafficInterceptorScreen
│   ├── terminal/      TerminalScreen
│   ├── adb/           AdbScreen
│   ├── autoadb/       AutoAdbScreen
│   └── common/        HunterTopBar, HunterCard
├── monitor/
│   ├── BroadcastMonitorScreen + Receiver
│   ├── TaskHijackScreen
│   └── AccessibilityMonitorScreen + Service
└── web/               WebTesterScreen
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.x |
| UI Framework | Jetpack Compose (Material 3) |
| Navigation | Navigation Compose (type-safe, `@Serializable`) |
| Concurrency | Kotlin Coroutines + `Dispatchers.IO` |
| APK Parsing | `ZipFile` + `XmlPullParser` (no external libs) |
| Proxy Server | Raw `ServerSocket` on `127.0.0.1:8877` |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |
| Build System | Gradle 8.9 + AGP 8.x |
| Java Version | 17 |

---

## Requirements

- Android 10+ (API 29+)
- No root required for most features
- Root / `run-as`: enables SharedPrefs reading on non-debug apps
- ADB over USB: required for ADB Manager commands
- Frida server on device: required for Frida script execution (scripts generated on-device, run from PC)

---

## Build Instructions
```bash
git clone https://github.com/ynsmroztas/AndroHunter.git
cd AndroHunter

# Requires Java 17
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Usage Guide

### Traffic Interception
```bash
# Start proxy in app → tap ▶ START
# Configure proxy on device Wi-Fi: 127.0.0.1:8877

# Or via ADB:
adb shell settings put global http_proxy 127.0.0.1:8877

# Use target app — requests appear in real time
# Tap any entry for full details + curl command

# Remove proxy when done:
adb shell settings put global http_proxy :0
```

### Frida Script Usage
```bash
# Push frida-server to device
adb push frida-server /data/local/tmp/
adb shell chmod 755 /data/local/tmp/frida-server
adb shell /data/local/tmp/frida-server &

# Copy generated script from app, then:
frida -U -f com.target.app -l script.js --no-pause
```

### FileProvider Path Traversal
```bash
# Via ADB (copy from ADB Commands tab in app):
adb shell content read --uri 'content://com.target.app.fileprovider/files/../../../data/data/com.target.app/databases/'
```

---

## Vulnerability Categories Covered

| Category | Modules |
|----------|---------|
| Hardcoded Secrets | DEX Analyzer |
| Exported Components | Manifest Viewer, Intent Fuzzer, Activity Launcher |
| SQL Injection | Content Provider Fuzzer |
| Path Traversal | FileProvider Analyzer, Intent Fuzzer |
| Insecure Deep Links | Intent Fuzzer, Payload Engine, Activity Launcher |
| OAuth Redirect Hijack | Payload Engine |
| Broadcast Injection | Broadcast Fuzzer, Broadcast Monitor |
| SSL Pinning | SSL Bypass Guide, Frida Generator |
| Sensitive Data Storage | Shared Preferences Reader |
| StrandHogg Task Hijack | Task Hijack Module |
| HTTP Traffic Analysis | Traffic Interceptor |
| Root Detection Bypass | Frida Generator |
| Cryptographic Weaknesses | Frida Crypto Monitor |

---

## Roadmap

- [ ] HTTPS MITM with dynamic certificate generation
- [ ] SQLite database browser (root)
- [ ] Report export (Markdown / JSON)
- [ ] AppDetail v4 module integration (single hub screen)
- [ ] Custom payload library
- [ ] Burp Suite upstream proxy chaining

---

## Author

**ynsmroztas** — [@ynsmroztas](https://x.com/ynsmroztas) · MITSEC
Bug bounty hunter · Yes We Hack · HackerOne

---

## License

MIT License — Copyright (c) 2025 ynsmroztas
