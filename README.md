# App Block

A small, private-by-design Android app that keeps selected apps out of reach during a recurring time window. Pick apps such as Instagram or Health Connect, choose the days and hours, and App Block shows a calm block screen if one of them is opened during that window.

## Features

- Pick any launchable app installed on the phone
- Configure start and end times, including windows that cross midnight
- Choose individual weekdays
- Pause the schedule without losing the configuration
- No internet permission, accounts, analytics, or cloud storage
- Automatic signed APK release for every push to `main`

## Install

1. Open the repository's **Releases** page on your Android phone.
2. Download `app-block.apk` from the latest release.
3. Allow installation from your browser when Android asks, then install the APK.
4. Open App Block, choose the apps and schedule, and tap **Open Accessibility settings**.
5. Select **App Block** and enable its service.

Future APKs from this repository use the same signing key and a higher version code, so they install as updates without removing your settings.

> [!NOTE]
> Android requires an Accessibility Service for immediate app-launch detection. App Block listens only for the package name of the foreground app. It cannot read window content, does not request internet access, and stores its configuration only on the device. This is a focus aid rather than parental-control software: anyone with access to system settings can disable the service or uninstall the app.

## Build locally

Requirements: JDK 17 and Android SDK 37.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The installable debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release automation

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs tests, builds a signed release APK, and creates a versioned GitHub release on every push to `main`. The signing key and passwords are stored as encrypted GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The original signing key should be backed up securely. Losing it means later APKs cannot update an existing installation.

## License

MIT
