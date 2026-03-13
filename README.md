<div align="center">

# 🎵 InnerTuneStyle

### ✨ A modern, modular Android music app scaffold inspired by InnerTune architecture

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

</div>

---

## 🚀 Overview

**InnerTuneStyle** is a clean, scalable Android starter with an **InnerTune-inspired module layout**.
It is built for fast iteration, feature isolation, and easy long-term maintenance.

### ✅ Highlights

- 🧩 **Modular architecture** (`app`, `core`, `feature`)
- 🎨 **Compose-ready UI layer** for modern Android development
- 📦 **Version catalog** via `libs.versions.toml`
- ⚙️ **Kotlin DSL Gradle setup** (`*.gradle.kts`)
- 🔧 **Wrapper bootstrap helper** for restricted environments

---

## 🗂 Project Structure

```text
.
├── app/                      # Android application module
├── core/
│   ├── model/                # Shared models
│   └── data/                 # Shared repositories/data sources
├── feature/
│   └── home/                 # Home feature module
├── gradle/
│   ├── libs.versions.toml    # Centralized dependency versions
│   └── wrapper/
├── build.gradle.kts          # Root build config
├── settings.gradle.kts       # Module includes + repositories
└── scripts_fetch_gradle_binary.sh
```

---

## 🧱 Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Build:** Gradle + Kotlin DSL
- **Architecture Direction:** Feature + Core modular split

---


## 🔁 Sync with OpenTune upstream

To pull in the full file/folder layout from [`Arturo254/OpenTune`](https://github.com/Arturo254/OpenTune), run:

```bash
./scripts_sync_opentune.sh
```

Other supported workflows:

```bash
# specific branch
./scripts_sync_opentune.sh main

# if GitHub is blocked, import from a local clone
./scripts_sync_opentune.sh --from-local /path/to/OpenTune

# import from downloaded archives
./scripts_sync_opentune.sh --from-tar /path/to/OpenTune.tar.gz
./scripts_sync_opentune.sh --from-zip /path/to/OpenTune.zip

# preview changes only
./scripts_sync_opentune.sh --dry-run --from-local /path/to/OpenTune
```

The script mirrors upstream content into this repository (excluding `.git`).

---

## ▶️ Getting Started

### 1) Clone

```bash
git clone <your-repo-url>
cd M3-Play
```

### 2) (Optional) Seed Gradle Wrapper Locally

If your environment blocks downloads from `services.gradle.org`:

```bash
./scripts_fetch_gradle_binary.sh
```

### 3) Build Debug APK

```bash
./gradlew assembleDebug
```

---

## 🧩 Modules at a Glance

| Module | Responsibility |
|---|---|
| `:app` | Application entry point, manifest, app wiring |
| `:core:model` | Shared domain models |
| `:core:data` | Shared repositories and data sources |
| `:feature:home` | Home UI feature implementation |

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues and PRs for:

- new features
- architecture improvements
- UI polish
- testing and CI upgrades

---

<div align="center">

### 💫 Built with Kotlin, Compose, and clean modular vibes

</div>
