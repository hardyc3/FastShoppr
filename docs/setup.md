# Development Environment Setup

This document describes the workstation setup needed to build, run, and test
FastShoppr on any computer. It applies to every phase. It is not a phase
deliverable and does not authorize starting a phase.

> Phase 0 has no Gradle project yet. Until Phase 1 creates the wrapper, use the
> standalone Gradle installation only for throwaway spike code. Do not commit
> build output, `local.properties`, keystores, or spike projects.

## Required versions

| Component | Required | Notes |
|---|---|---|
| JDK | **21** | Project baseline. Android Studio's bundled JBR is for the IDE, not Gradle. |
| Android Studio | Latest stable | Tested with Android Studio 2026.2.2. IntelliJ IDEA with the Android plugin also works. |
| Android SDK Platform | **36** | Matches `compileSdk` / `targetSdk`. |
| Android SDK Build-Tools | **36.1.0** | Pin this in the project's version catalog in Phase 1. |
| Android SDK Platform-Tools | Latest stable | Provides `adb`. |
| Android Emulator | Latest stable | Requires hardware virtualization. |
| Emulator system image | API 36 Google APIs | `x86_64` on Intel/AMD, `arm64-v8a` on Apple Silicon. |
| Gradle | **8.10.2** for now | Phase 1 adds the committed Gradle wrapper. Use `./gradlew` after that. AGP 8.8.2 requires Gradle ≥ 8.10.2 (Phase 0 finding). |
| Git | Any current version | Required for normal workflow. |

`google_apis` is preferred over `google_apis_playstore` for automation. It
includes Google Play services, which ML Kit uses for model downloads, without
requiring a Play Store account.

## Workstation requirements

- 64-bit Windows 10/11, macOS 12+, or a modern Linux distribution.
- Hardware virtualization enabled in firmware:
  - Windows: WHPX (Hyper-V platform) or an equivalent accelerator.
  - Linux: KVM.
  - macOS: Hypervisor.framework.
- At least **16 GB RAM** recommended. 8 GB is tight when the IDE, Gradle, and an
  emulator run together.
- At least **20 GB free disk space** for the IDE, SDK, system image, and AVD.
- Internet access for the initial SDK and dependency downloads.

## Windows setup

### 1. Install Android Studio

Install the latest stable Android Studio from
<https://developer.android.com/studio>. Use the setup wizard to install:

- Android SDK
- Android SDK Platform 36
- Android SDK Build-Tools 36.1.0
- Android SDK Platform-Tools
- Android Emulator
- An API 36 Google APIs system image

The default SDK location is:

```text
%LOCALAPPDATA%\Android\Sdk
```

### 2. Install JDK 21

Install Eclipse Temurin 21 (or Microsoft Build of OpenJDK 21). The tested
package is:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --exact
```

Typical install path:

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot
```

Replace the version with the one actually installed.

### 3. Set environment variables

Run in PowerShell, replacing the JDK path with the installed version:

```powershell
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'

[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdk, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $sdk, 'User')

$path = [Environment]::GetEnvironmentVariable('Path', 'User')
$parts = @($path -split ';' | Where-Object { $_ })
$parts += (Join-Path $sdk 'platform-tools')
$parts += (Join-Path $sdk 'emulator')
$parts += (Join-Path $sdk 'cmdline-tools\latest\bin')
[Environment]::SetEnvironmentVariable('Path', ($parts -join ';'), 'User')
```

Close and reopen terminals and the IDE after changing these variables.

### 4. Install the SDK command-line tools (optional but recommended)

In Android Studio: **SDK Manager → SDK Tools → Android SDK Command-line Tools
(latest)**. This provides `sdkmanager`, `avdmanager`, and the newer `android`
CLI.

If installing manually, download the Windows command-line tools from
<https://developer.android.com/studio#command-line-tools-only>, verify the
published SHA-1, and extract so the layout is:

```text
%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat
%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat
```

### 5. Install SDK packages from the command line

With the classic `sdkmanager`:

```powershell
sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;36.1.0" "emulator" "system-images;android-36;google_apis;x86_64"
```

Newer command-line tools (23.0.0+) forward `sdkmanager` to the `android` CLI.
In that case use slash paths:

```powershell
android sdk install "platform-tools" "platforms/android-36" "build-tools/36.1.0" "emulator" "system-images/android-36/google_apis/x86_64"
```

### 6. Create and verify an AVD

```powershell
avdmanager create avd -n FastShoppr_API_36 -k "system-images;android-36;google_apis;x86_64" -d pixel_9
emulator -avd FastShoppr_API_36
adb devices
adb shell getprop ro.build.version.sdk
```

The last command should print `36`. The first boot can take several minutes on
a laptop.

## macOS setup

1. Install the latest stable Android Studio.
2. Install JDK 21, for example:

   ```bash
   brew install --cask temurin@21
   ```

3. Add to `~/.zshrc`:

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
   ```

4. Install packages. On Apple Silicon use the ARM image:

   ```bash
   sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;36.1.0" "emulator" "system-images;android-36;google_apis;arm64-v8a"
   avdmanager create avd -n FastShoppr_API_36 -k "system-images;android-36;google_apis;arm64-v8a" -d pixel_9
   ```

## Linux setup

1. Install the latest stable Android Studio.
2. Install JDK 21 from your distribution or Adoptium.
3. Install KVM and add your user to the `kvm` group:

   ```bash
   sudo apt install qemu-kvm
   sudo usermod -aG kvm "$USER"
   ```

   Log out and back in after changing groups.
4. Add to `~/.bashrc`:

   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   export ANDROID_HOME="$HOME/Android/Sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
   ```

5. Install packages and create the AVD with the `x86_64` image.
6. For a physical device, install the Android udev rules and confirm
   `adb devices` shows `device`, not `unauthorized`.

## WSL2 notes

WSL2 does not expose `/dev/kvm`, so the Android emulator must run on the
Windows host, not inside WSL.

- Run Android Studio, the emulator, and Gradle builds on Windows.
- Do not use a WSL JDK 25 for Android builds; Gradle/AGP may reject it.
- If working from WSL, call the Windows tools explicitly:

  ```bash
  export ANDROID_HOME="/mnt/c/Users/$USER/AppData/Local/Android/Sdk"
  export PATH="$PATH:$ANDROID_HOME/platform-tools"
  adb.exe devices
  ```

- The repository can stay on `/mnt/c` and be edited from either side, but build
  from the Windows side to keep the SDK and emulator in one environment.

## IDE setup

### Android Studio

- **Settings → Languages & Frameworks → Android SDK**: point at the SDK
  directory and confirm Platform 36 and Build-Tools 36.1.0 are installed.
- **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle
  JDK**: select the installed **JDK 21**, not the IDE's bundled JBR.
- Do not add Kotlin or Compose plugins or source sets. The app is Java-only;
  Gradle Kotlin DSL build scripts are allowed.

### IntelliJ IDEA

IntelliJ IDEA Community or Ultimate with the Android plugin is a valid
alternative. Use the same SDK and JDK 21 settings. For IntelliJ 2024.1, use an
AGP version compatible with that IDE for any spike project; Android Studio is
the primary IDE for the Phase 1 project.

## Verify the toolchain

```bash
java -version          # 21.x
adb version
sdkmanager --list_installed
emulator -list-avds
adb devices
adb shell getprop ro.build.version.sdk   # 36
```

For Gradle, after Phase 1 adds the wrapper:

```bash
./gradlew --version     # JVM must be 21.x
```

Before the wrapper exists:

```bash
gradle --version        # JVM must be 21.x
```

## Project build, run, and test commands

These commands become available when the Phase 1 Gradle project exists. They
are the required commands from `AGENTS.md`:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew spotlessCheck
./gradlew connectedDebugAndroidTest   # requires a running emulator or device
```

A phase is not complete unless the relevant commands pass.

## Physical-device requirements

Most development can use the emulator. Two things cannot:

- **Gemini Nano / AICore** requires a supported physical device. The emulator
  does not include AICore, so `checkStatus()` reports unavailable and latency,
  quota, and foreground-only behavior cannot be measured.
- **Final retailer behavior** should be confirmed on a real device when
  possible. Walmart may treat an emulator differently from a phone.

Gemini Nano requirements:

- A device on Google's current supported list (for example Pixel 9/10/11,
  Galaxy S25/S26, or another listed model).
- Android 15/16 with current Google Play services and AICore.
- **Locked bootloader.** Unlocked or rooted devices are not supported.
- A Google account and region where the model is available.
- The app must be the top foreground app; inference is blocked in the
  background, including from a foreground service.

If no supported device is available, record that as a Phase 0 no-go input. Do
not side-load AICore or modify a system image to fake support.

## Network and retailer notes

- Home Depot may return **HTTP 403** or a bot challenge from some networks.
  Treat that as a finding, not something to bypass. Capture a sanitized blocked
  page fixture if possible.
- Walmart may show a CAPTCHA, sign-in wall, or different content in an emulator.
  Record the behavior and test on a real device when available.
- Never commit cookies, tokens, addresses, or live personal data. Sanitize all
  fixtures.
- Live retailer and live AI tests are opt-in and must not run in CI.

## Troubleshooting

| Symptom | Check |
|---|---|
| `adb` not found | Add `%ANDROID_HOME%\platform-tools` to `PATH`; reopen the terminal. |
| `JAVA_HOME` points at JDK 22/25 | Set it to JDK 21 and restart the IDE. Gradle reports the JVM it actually uses. |
| Gradle uses the wrong JDK | Set **Gradle JDK** in the IDE and check `./gradlew --version`. |
| Emulator says no acceleration | Run `emulator-check accel`. On Windows enable WHPX/Hyper-V platform; on Linux enable KVM. |
| Emulator is very slow | Use a `google_apis` image, not a Play Store image, for automation; close other apps; do not use a snapshot for the first boot. |
| `adb devices` shows `unauthorized` | Accept the USB debugging prompt on the phone; revoke and re-authorize if needed. |
| SDK package not found | Use the package ID matching your tools: `platforms;android-36` for classic `sdkmanager`, `platforms/android-36` for the newer `android` CLI. |
| `platform-tools` duplicate warning | Remove an old `platform-tools.backup` directory only if you are sure it is not needed. |
| Build fails with `local.properties` missing | Let the IDE create it, or set `sdk.dir` to the SDK path. Never commit it. |
| Home Depot returns 403 | Do not bypass. Record the blocked page and use a sanitized fixture. |

## Reference installation

This laptop was verified on **2026-11-03** with:

| Item | Value |
|---|---|
| OS | Windows 10 Pro 22H2, WSL2 available |
| CPU / RAM | Intel i5-7200U, 16 GB RAM |
| Android Studio | 2026.2.2, build `AI-262.10968.63.2622.16730706` |
| JDK | Eclipse Temurin 21.0.13+9 |
| Gradle | 8.8 |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` |
| SDK packages | `platforms;android-36`, `build-tools;36.1.0`, `platform-tools;37.0.1`, `emulator;37.2.13`, `cmdline-tools;latest`, `system-images;android-36;google_apis;x86_64` |
| AVD | `FastShoppr_API_36` (Pixel 9, API 36, Google APIs, x86_64) |
| Emulator check | WHPX installed and usable |
| Verification | Emulator booted to API 36; Google Play services and WebView present; Gradle 8.8 ran on JVM 21 |
| Not verified | Gemini Nano on a physical device (none connected) |

## First-time checklist

- [ ] JDK 21 installed and selected for Gradle.
- [ ] Android Studio and Android SDK installed.
- [ ] Platform 36, Build-Tools 36.1.0, Platform-Tools, Emulator installed.
- [ ] API 36 Google APIs system image installed.
- [ ] AVD created and boots.
- [ ] `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` set.
- [ ] `java -version` reports 21.
- [ ] `adb devices` works.
- [ ] Gradle reports JVM 21.
- [ ] A supported physical device is available if Gemini Nano work is in scope.
- [ ] `local.properties` and build outputs are not committed.
