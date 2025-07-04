# MIDPlay

A feature-rich online music player application developed using J2ME (Java Micro Edition) for mobile devices.

![AppIcon](/res/images/AppIcon.png)

## Features

- Stream music from multiple sources (NCT Music, SoundCloud)
- Browse music by categories and playlists
- Search for songs, artists, and albums
- Create and manage favorites
- Multi-language support
- Low resource consumption for feature phones

## System Requirements

- Device supporting J2ME MIDP 2.0
- CLDC 1.1
- Minimum 512KB heap memory
- Network connectivity for streaming

## Installation

1. Download the latest `.jar` file from the [Releases](https://github.com/phd051199/MIDPlay/releases) page
2. Install on your J2ME compatible device

## Usage

1. Launch the application
2. Navigate using the main menu
3. Browse categories or search for music
4. Select a song to play
5. Use player controls to manage playback

## Building from Source

```bash
# Requires NetBeans with Mobility Pack or equivalent J2ME development tools
ant build
```

## Technologies Used

- Java ME (J2ME) MIDP 2.0
- CLDC 1.1
- Record Management System (RMS) for local storage
- RESTful API connections for music streaming

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Author

Duy Pham
