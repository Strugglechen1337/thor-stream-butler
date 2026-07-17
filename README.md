<div align="center">
  <img src="docs/screenshots/dashboard-landscape.svg" width="860" alt="Thor Stream Butler dashboard concept in landscape orientation">
  <h1>Thor Stream Butler</h1>
  <p><strong>Stream. Check. Launch.</strong></p>
  <p>The local, controller-first game-streaming launcher for Android handhelds.</p>
  <p>
    <a href="https://strugglechen1337.github.io/thor-stream-butler/"><strong>Project website</strong></a>
    ·
    <a href="#build"><strong>Build guide</strong></a>
    ·
    <a href="https://github.com/Strugglechen1337/thor-stream-butler/issues"><strong>Issues</strong></a>
    ·
    <a href="#optional-support"><strong>Support</strong></a>
  </p>
  <p>
    <a href="https://github.com/Strugglechen1337/thor-stream-butler/actions/workflows/android-ci.yml"><img src="https://github.com/Strugglechen1337/thor-stream-butler/actions/workflows/android-ci.yml/badge.svg" alt="Android CI"></a>
    <a href="https://github.com/Strugglechen1337/thor-stream-butler/actions/workflows/pages.yml"><img src="https://github.com/Strugglechen1337/thor-stream-butler/actions/workflows/pages.yml/badge.svg" alt="GitHub Pages"></a>
    <img src="https://img.shields.io/badge/Android-28--37-3DDC84?logo=android&logoColor=white" alt="Android API 28 through 37">
    <img src="https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 2.3.21">
    <img src="https://img.shields.io/badge/Tracking-none-4ADE80" alt="No tracking">
  </p>
</div>

Thor Stream Butler brings installed game-streaming apps together, optionally
checks network quality before launch, and manages local streaming hosts with
Wake-on-LAN. The current codebase is a compilable MVP with no account, cloud
backend, advertising, tracking, or telemetry.

It is built for Android gaming handhelds such as **AYN Thor / Odin** and
**Retroid Pocket**, while remaining usable on Android phones, tablets, and
similar devices running Android 9 or newer.

> Local-first by design. Measurements, hosts, settings, and history stay on the device.

## Screenshots

These are interface previews. They will be replaced with captures from tested
handheld hardware as the project moves toward its first release.

| Landscape dashboard | Portrait network test |
|---|---|
| ![Dashboard preview](docs/screenshots/dashboard-landscape.svg) | ![Network test preview](docs/screenshots/networktest-portrait.svg) |

## Features

- Adaptive launcher UI for portrait and landscape orientation
- Full controller, D-pad, keyboard, and touch support
- Large focusable tiles with a clearly visible focus ring
- Selection of launchable Android apps through `PackageManager`
- Local storage of display name, package name, category, icon reference, and order
- Launch-intent validation with a clear error when an app is unavailable
- Optional network check before opening a streaming app
- Green, yellow, red, or gray quality rating with explanations and recommendations
- Complete cancellable network test with live progress
- Local host management, TCP port checks, and Wake-on-LAN
- Room-backed history with a simple latency comparison to the previous measurement
- DataStore settings for launch behavior, diagnostics, and presentation

Initial streaming categories:

- GeForce NOW
- Xbox Cloud Gaming
- Xbox Remote Play / XBPlay
- PlayStation Remote Play / PXPlay
- Moonlight
- Steam Link
- Sunshine host
- Any user-selected Android app

Product names are used only to describe compatible categories. The project does
not bundle protected service logos. Icons for installed apps are loaded locally
through Android at runtime.

## How launch protection works

1. Focus and activate a launcher tile.
2. If enabled, Thor Stream Butler runs a short network check without a download test.
3. The app shows a color rating, explanation, and recommendation.
4. A green result can launch automatically after a short status display.
5. A yellow result displays a warning and then launches.
6. A red result offers **Launch anyway**, **Run again**, or **Cancel**.
7. A gray result means that Android or the network could not provide reliable measurements.

Every part of this flow can be adjusted in Settings.

## Network diagnostics

The diagnostic service uses public Android APIs and performs real measurement
attempts. Missing values stay `null` and are shown as unavailable or not
measurable; the app never invents plausible-looking results.

Depending on Android version, permissions, hardware, and network behavior, it can report:

- active transport: Ethernet, Wi-Fi, cellular, VPN, or another transport
- local IPv4 address and default gateway
- Wi-Fi SSID, frequency, link speed, and signal strength
- Android's validated internet status
- DNS resolution
- ping latency, jitter, and packet loss
- an optional short HTTPS download test
- reachability of an explicitly configured host and TCP port

Ping first uses Android's common `/system/bin/ping` executable with separate
process arguments. If it is unavailable, the service falls back to
`InetAddress.isReachable()`. Some devices and networks block ICMP; that is
reported as packet loss or an unavailable result instead of crashing the app.

Jitter is calculated as the average absolute difference between consecutive
successful latency measurements. Packet loss is
`(sent - received) / sent × 100`.

The optional download test is disabled by default. When enabled, the app
downloads time-limited test data over HTTPS from `speed.cloudflare.com`. The
provider can technically see the public IP address during that test. Thor
Stream Butler sends no account data and stores none of the response content.

## Quality rating

The quality evaluator lives in `core/network/QualityEvaluator.kt`, is independent
of Android components, and can be unit-tested directly. Thresholds are centralized
in `QualityThresholds` for later configurability.

Current defaults:

| Metric | Optimal / good | Warning | Problematic |
|---|---:|---:|---:|
| Latency | up to 30 ms | above 30 ms | above 60 ms |
| Jitter | up to 10 ms | above 10 ms | above 20 ms |
| Packet loss | 0% | above 0% | above 1% |
| Wi-Fi signal | 55% or higher | below 55% | below 35% |

The evaluator also considers transport type, 2.4/5/6 GHz Wi-Fi, validated
internet access, and host reachability. Download speed alone never determines
the overall rating. Ethernet and 5/6 GHz Wi-Fi are preferred; cellular and
2.4 GHz Wi-Fi produce a contextual note even when the other metrics are good.

## Technical foundation

- Kotlin with Coroutines and Flow
- Jetpack Compose and Material 3
- Gradle Kotlin DSL and a version catalog
- Minimum SDK 28
- Compile / target SDK 37 (Android 17)
- Android Gradle Plugin 9.3.0 with built-in Kotlin
- MVVM and Repository Pattern
- Hilt and KSP
- Room for launcher entries, hosts, and measurement history
- Preferences DataStore for settings
- Navigation Compose

WorkManager is deliberately not included in the MVP. Diagnostics and app launch
are explicit, visible user actions, and there is currently no reliable background
job that would justify the dependency.

## Project structure

The MVP uses a single `app` module. Package boundaries are designed so features
can later move into separate Gradle modules without a major rewrite.

```text
app/src/main/java/de/thorstream/butler/
├── core/
│   ├── common/          Result and error types
│   ├── designsystem/    Thor theme and colors
│   ├── network/         Calculations, rating, WOL packet
│   └── validation/      Host, IPv4, and MAC validation
├── data/
│   ├── database/        Room entities, DAOs, and database
│   ├── datastore/       Settings persistence
│   ├── di/              Hilt modules
│   ├── repository/      Android and Room repositories
│   └── service/         Network, ping, host, and WOL services
├── domain/
│   ├── model/           Android-independent models
│   ├── repository/      Repository interfaces
│   └── service/         Service interfaces
├── feature/
│   ├── dashboard/
│   ├── history/
│   ├── hosts/
│   ├── networktest/
│   └── settings/
└── navigation/          Adaptive app navigation
```

The central interfaces are `NetworkDiagnosticsService`, `PingService`,
`SpeedTestService`, `HostDiscoveryService`, `WakeOnLanService`,
`InstalledAppsRepository`, `StreamingEntryRepository`, and
`NetworkHistoryRepository`.

## Build

### Requirements

- JDK 17
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0 or newer
- Android Studio with Android 17 support, or an equivalent command-line SDK installation

The repository includes Gradle Wrapper 9.5.0; no global Gradle installation is required.

### Local SDK configuration

Create an untracked `local.properties` file in the project root:

```properties
sdk.dir=C\:\\Users\\NAME\\AppData\\Local\\Android\\Sdk
```

On macOS or Linux, for example:

```properties
sdk.dir=/Users/NAME/Library/Android/sdk
```

### Compile and test

Windows PowerShell:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebugAndroidTest
./gradlew.bat :app:lintDebug
```

macOS or Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

JVM tests run without a device. Instrumented Room repository tests are compiled
with `:app:assembleDebugAndroidTest` and can run on an emulator or device with
`connectedDebugAndroidTest`.

## Permissions

Dangerous permissions are requested only when the related feature is used.

| Permission | Android version | Purpose | Behavior without permission |
|---|---|---|---|
| `INTERNET` | All | DNS, ping fallback, TCP test, HTTPS test, Wake-on-LAN | Network diagnostics and host actions are unavailable |
| `ACCESS_NETWORK_STATE` | All | Active network, transport, and validated internet state | Basic diagnostics are unavailable |
| `ACCESS_WIFI_STATE` | All | Wi-Fi link data | Wi-Fi details are omitted |
| `ACCESS_COARSE_LOCATION` + `ACCESS_FINE_LOCATION` | Through Android 12L; manifest `maxSdkVersion=32` | SSID and Wi-Fi details on older versions; Android 12 requires both together | SSID and individual Wi-Fi values are omitted |
| `NEARBY_WIFI_DEVICES` | Android 13+ | Current Wi-Fi details, declared with `neverForLocation` | SSID and individual Wi-Fi values are omitted |
| `ACCESS_LOCAL_NETWORK` | Android 17+ when targeting SDK 37 | Explicit TCP/UDP communication with saved LAN hosts and Wake-on-LAN | Host tests and Wake-on-LAN are not executed; internet tests remain available |

`CHANGE_WIFI_MULTICAST_STATE` is intentionally not declared in the MVP because
there is no multicast or mDNS discovery. UDP broadcast to an explicitly
configured Wake-on-LAN target does not require it. The permission decision must
be reviewed if automatic host discovery is added later.

The manifest's `<queries>` block is not a runtime permission. It limits package
visibility to activities with a launcher intent.

Further reading: [Android local network permission](https://developer.android.com/privacy-and-security/local-network-permission),
[Android Wi-Fi permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions).

## Privacy and security

- No account or cloud backend
- No advertising, trackers, telemetry, or analytics SDKs
- No stored credentials
- No hidden Android APIs and no root access
- Hosts, network data, settings, and history stay in the private app sandbox
- Android backup is disabled for the MVP (`allowBackup=false`)
- Cleartext HTTP is disabled; the optional download test uses HTTPS
- Logs do not contain IP addresses, SSIDs, or MAC addresses
- Network operations use timeouts and respect coroutine cancellation

Uninstalling the app removes the local database and DataStore settings through
Android. Measurement history can also be deleted separately inside the app.

## Known Android limitations

- Android or the device manufacturer may redact SSID and Wi-Fi metadata even after permission is granted.
- Wi-Fi link speed is a negotiated PHY value, not a real download-speed measurement.
- A target that blocks ICMP can look like packet loss. The reachability fallback varies by device.
- `NET_CAPABILITY_VALIDATED` is Android's view of internet access, not a guarantee that a particular streaming service is available.
- Wake-on-LAN only works when the host, firmware, network adapter, and router support broadcast or magic packets.
- The default `255.255.255.255` broadcast does not work on every network; each host can use a subnet broadcast or unicast target instead.
- Demo tiles use common package names. Manufacturer or store variants may use different identifiers and appear as not installed.
- The MVP history is a list of the latest 100 measurements. Full charts are not implemented yet.
- Automatic Sunshine/Moonlight discovery and port scanning are explicitly outside the MVP.

## Tests

The test suite covers:

- quality evaluation across green, yellow, red, and gray outcomes
- jitter and packet-loss calculations
- IPv4, hostname, and MAC validation
- exact construction of the 102-byte Wake-on-LAN packet
- successful and failed network-test ViewModel flows
- no-active-network errors without invented measurements
- Room repositories for launcher tiles, hosts, and history

Test fakes implement network service and settings interfaces without Android
network access. GitHub Actions builds the app, runs unit tests and lint, compiles
instrumented tests, and uploads the debug APK and reports.

## Roadmap after the MVP

- Automatic Sunshine and Moonlight host discovery through an Android 17-compliant, privacy-friendly flow
- Port scanning only for explicitly entered hosts
- Profiles per streaming service
- Recommended resolution and bitrate
- Separate Wi-Fi and Ethernet profiles
- Widgets and a Quick Settings tile
- Configuration export and import
- Backup to a local NAS
- Controller mapping test
- Streaming-session timer
- Battery and temperature monitoring
- VPN-aware evaluation and UI
- Comparison of multiple Wi-Fi networks
- Notifications for unstable connections
- Optional Windows companion service

## Transparency: AI-assisted development

The initial application, architecture, tests, documentation, and project website
were developed in collaboration with OpenAI Codex. The complete implementation
is public for review, and bug reports, focused pull requests, and technical
feedback are welcome.

## Thor family

Thor Stream Butler follows the same local-first and handheld-first philosophy as
[Thor ROM Butler](https://github.com/Strugglechen1337/ThorROMButler), the Android
assistant for organizing, checking, patching, and maintaining ROM collections.

## Optional support

Thor Stream Butler stays free of advertising, tracking, and telemetry. If the
project helps you and you would like to say thanks voluntarily, you can buy me a
coffee:

[paypal.me/marcelstrohmeyer](https://paypal.me/marcelstrohmeyer)
