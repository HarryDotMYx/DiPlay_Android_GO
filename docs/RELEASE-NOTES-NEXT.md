# DiPlay next release notes

Changes through DiPlay 0.2.19 are documented in [0.2.19 release notes](RELEASE-NOTES-0.2.19.md). Measured checks are in [VALIDATION.md](VALIDATION.md). Device acceptance and remaining failure families are tracked in [connection reliability validation](CONNECTION_RELIABILITY.md).

## Compatibility

- DiPlay installs on Android 6.0 (API 23). Some 8227L head units show "Android 8.1 Go" in their settings but report API level 23, and Android refused the 0.2.19 APK there with "There was a problem parsing the package". The calls that Android 6.0 lacks now have fallbacks: the settings buttons work out their corner radius again, the media path keeps its decoder and microphone bookkeeping without the Android 7.0 collection methods, AudioTrack underruns read as 0, the microphone uses its fallback capture latency, and wired lockdown TLS skips the endpoint identification setting, which an Android 6 engine never applies. WorkManager is 2.11.2, the last release for API 23. BYD ambient lighting stays on Android 7.0 and newer, which every BYD head unit runs. Android 6.0–8.1 still need vehicle testing, and USB, Bluetooth and Wi-Fi Direct on Android 6 firmware are untested.
