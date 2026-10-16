# MIDPlay

![AppIcon](/res/Icon.png)

An online music player for J2ME (Java ME) mobile devices — CLDC 1.1 / MIDP 2.0.

## Features

- **Multi-source streaming**
- **Discovery** — browse by category and playlist, search songs / artists / albums
- **Playback** — seek, resume position, sleep timer
- **Library** — favorites, playlists, recent history
- **Localization** — English, Brazilian Portuguese, Vietnamese, Turkish, Polish, Hebrew
- **MPGram** — embedded Telegram client that returns to MIDPlay without stopping playback

## Fork Features

This fork adds features on top of the original MIDPlay project:

- **Download music** to the device for offline access.
- **File explorer** to browse local storage, view text files and images, play local audio, and add local tracks to playlists.
- **Notepad** with multiple notes and VNOTE import/export.
- **Open external links** using the device's supported link handlers.
- **Embedded MPGram integration** for devices that cannot conveniently switch between apps. MPGram is integrated into MIDPlay, and returning to the player keeps music playback running. See the [MPGram client](https://github.com/shinovon/mpgram-client).
- **Reorder tracks** in custom playlists.

## Requirements

- J2ME device supporting MIDP 2.0 / CLDC 1.1
- Network connectivity for streaming

## Install

1. Download the latest `.jar` from the [Releases](https://github.com/phd051199/MIDPlay/releases) page
2. Install on a J2ME-compatible device (or load in an emulator such as KEmulator)

## Build

```bash
git submodule update --init --recursive
./build.sh
```

Requires **JDK 8** (the last toolchain emitting CLDC-compatible bytecode) and Python 3 to prepare the embedded generic MPGram sources. Output: `dist/MIDPlay.jar` + `dist/MIDPlay.jad`. See `build.sh` for the full pipeline (compile → package → ProGuard → JAD).

## Tech Stack

- Java ME (J2ME), MIDP 2.0 / CLDC 1.1
- Record Management System (RMS) for local storage
- REST APIs for streaming and metadata

## Contributing

**Code:** fork → feature branch → commit → push → open a Pull Request.

**Language:** duplicate `langs/en.json`, translate, and submit via PR or an `[Enhancement]` issue.

## License

MIT — see [LICENSE](LICENSE).
