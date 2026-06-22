# MIDPlay

![AppIcon](/res/Icon.png)

An online music player for J2ME (Java ME) mobile devices — CLDC 1.1 / MIDP 2.0.

## Features

- **Multi-source streaming** — NCT, SoundCloud, YouTube Music, Spotify
- **Discovery** — browse by category and playlist, search songs / artists / albums
- **Playback** — seek, resume position, sleep timer
- **Library** — favorites, playlists, recent history
- **Localization** — English, Vietnamese, Turkish, Polish, Hebrew

## Requirements

- J2ME device supporting MIDP 2.0 / CLDC 1.1
- Network connectivity for streaming

## Install

1. Download the latest `.jar` from the [Releases](https://github.com/phd051199/MIDPlay/releases) page
2. Install on a J2ME-compatible device (or load in an emulator such as KEmulator)

## Build

```bash
./build.sh
```

Requires **JDK 8** (the last toolchain emitting CLDC-compatible bytecode). Output: `dist/MIDPlay.jar` + `dist/MIDPlay.jad`. See `build.sh` for the full pipeline (compile → package → ProGuard → JAD).

## Tech Stack

- Java ME (J2ME), MIDP 2.0 / CLDC 1.1
- Record Management System (RMS) for local storage
- REST APIs for streaming and metadata

## Contributing

**Code:** fork → feature branch → commit → push → open a Pull Request.

**Language:** duplicate `langs/en.json`, translate, and submit via PR or an `[Enhancement]` issue.

## License

MIT — see [LICENSE](LICENSE).
