# FontBook

FontBook is an offline Android browser and inspector for the fonts supplied by
the device. It provides searchable font specimens, custom text, multilingual
samples, glyph coverage, metrics, and OpenType metadata.

The app requires Android 8.0 (API 26) or newer and does not request network or
storage permissions.

## Build

```sh
./gradlew test lint assembleDebug
```

## Google Play releases

The repository includes a GitHub Actions workflow that builds and signs an
Android App Bundle, saves it as a workflow artifact, and can upload it to a
Google Play testing or production track. See
[docs/play-store-publishing.md](docs/play-store-publishing.md) for the one-time
Play Console, signing, and GitHub setup.
