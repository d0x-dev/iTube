<img width="1774" height="887" alt="image" src="https://github.com/user-attachments/assets/0623281b-ac28-4f80-8749-694c0eba0358" />

# iTube

An open-source Android app for watching YouTube videos and Shorts, and listening to music. iTube combines video discovery, a full-featured player, and iMusic in one native Android experience.

[![Latest release](https://img.shields.io/github/v/release/d0x-dev/iTube?label=release)](https://github.com/d0x-dev/iTube/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

## Preview
![iTube player preview](https://github.com/user-attachments/assets/eefb57f5-8384-4aa0-8bf2-a47e41231c12)

## Features

- Browse and search videos, Shorts, channels, playlists, and music.
- Watch videos with adjustable playback speed and quality, captions, comments, and a floating mini-player.
- Explore Shorts in a dedicated swipeable player.
- Listen with iMusic, background playback, Android media controls, queues, and lyrics.
- Discover channels, albums, releases, and playlists.
- Choose light, dark, and AMOLED appearances.
- Sign in to access supported YouTube account features.

## Install

Open [GitHub Releases](https://github.com/d0x-dev/iTube/releases) and download the APK attached to the release you want. Release APKs use this filename format:

```text
iTube_v<TAG>_signed.apk
```

For example, the APK for tag `2.0.0` is `iTube_v2.0.0_signed.apk`.

## Build from source

### Requirements

- Android Studio with Android SDK 36
- JDK 17

Clone the repository and build a debug APK:

```bash
git clone https://github.com/d0x-dev/iTube.git
cd iTube
./gradlew assembleDebug
```

On Windows, run `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/iTube.apk`.

## Tech

iTube is written in Kotlin and uses Jetpack Compose for its interface and AndroidX Media3 for playback. The Android app module is under `app/`.

## License

iTube is distributed under the [GNU General Public License v3.0](LICENSE).
