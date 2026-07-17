# Contributing to Thor Stream Butler

**English** | [Deutsch](#deutsch)

## English

Thank you for your interest. Contributions should preserve the project's
local-first, controller-first approach.

### Before contributing

1. Search existing issues and pull requests for related discussions.
2. Open a feature issue before starting a large or behavior-changing contribution.
3. Never publish IP addresses, SSIDs, MAC addresses, hostnames, credentials, or other private network data.

### Development environment

- JDK 17
- Android SDK Platform 37
- Android Build Tools 36.0.0 or newer
- Android Studio or the included Gradle Wrapper

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Instrumented Room tests are compiled with `:app:assembleDebugAndroidTest` and
run on an emulator or device with `connectedDebugAndroidTest`.

### Architecture rules

- Keep rating and validation logic independent of Android components.
- Access persistence and networking through repository and service interfaces.
- Keep unmeasurable values `null`; never simulate plausible network measurements.
- Give every network operation a timeout and clean coroutine-cancellation behavior.
- Request dangerous permissions only when the related feature is triggered.
- New UI must work in portrait and landscape with controller, D-pad, keyboard, and touch input.
- Do not add trackers, advertising, telemetry, unnecessary permissions, hidden APIs, or root-only behavior.
- Store network information locally and keep sensitive values out of logs.

### Pull requests

- Keep each change focused and use understandable commits.
- Add tests for new domain logic and error cases.
- Explain user-visible behavior and include sanitized screenshots for UI changes.
- Run unit tests, lint, the debug build, and the instrumented-test build when relevant.
- Update English and German documentation when behavior, permissions, or known limitations change.

Product names may be used to describe supported services. Protected logos or
other third-party assets require documented permission before they can be added.

### Optional support

Contributions are always more valuable than donations. If you prefer to support
development financially, the voluntary PayPal.me link is listed on the
[project website](https://strugglechen1337.github.io/thor-stream-butler/#support).

---

## Deutsch

Danke für dein Interesse. Beiträge sollen den local-first und controller-first
Ansatz des Projekts erhalten.

### Vor einem Beitrag

1. Suche in bestehenden Issues und Pull Requests nach ähnlichen Diskussionen.
2. Eröffne vor einer großen oder verhaltensändernden Anpassung ein Feature-Issue.
3. Veröffentliche niemals IP-Adressen, SSIDs, MAC-Adressen, Hostnamen, Zugangsdaten oder andere private Netzwerkdaten.

### Entwicklungsumgebung

- JDK 17
- Android SDK Platform 37
- Android Build Tools 36.0.0 oder neuer
- Android Studio oder der enthaltene Gradle Wrapper

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Instrumentierte Room-Tests werden mit `:app:assembleDebugAndroidTest` gebaut
und mit `connectedDebugAndroidTest` auf einem Emulator oder Gerät ausgeführt.

### Architekturregeln

- Bewertungs- und Validierungslogik bleibt unabhängig von Android-Komponenten.
- Persistenz und Netzwerkzugriff erfolgen über Repository- und Service-Interfaces.
- Nicht messbare Werte bleiben `null`; simuliere niemals plausible Netzwerkmessungen.
- Netzwerkoperationen benötigen Timeouts und sauberes Coroutine-Cancellation-Handling.
- Gefährliche Berechtigungen werden erst beim Auslösen der zugehörigen Funktion angefragt.
- Neue UI muss in Portrait und Landscape per Controller, D-Pad, Tastatur und Touch bedienbar sein.
- Keine Tracker, Werbung, Telemetrie, unnötigen Berechtigungen, versteckten APIs oder Root-Abhängigkeiten.
- Netzwerkdaten bleiben lokal und sensible Werte gehören nicht in Logs.

### Pull Requests

- Halte jede Änderung fokussiert und verwende nachvollziehbare Commits.
- Ergänze Tests für neue Domain-Logik und Fehlerfälle.
- Erkläre sichtbares Verhalten und füge bei UI-Änderungen bereinigte Screenshots hinzu.
- Führe Unit Tests, Lint, Debug-Build und gegebenenfalls den instrumentierten Test-Build aus.
- Aktualisiere öffentliche GitHub-Texte und Release Notes immer gleichwertig auf Englisch und Deutsch.

Produktnamen dürfen zur Beschreibung unterstützter Dienste verwendet werden.
Geschützte Logos oder andere Drittanbieter-Assets benötigen eine dokumentierte
Nutzungsberechtigung.

### Optional unterstützen

Beiträge sind immer wertvoller als Spenden. Wer die Entwicklung lieber
finanziell unterstützen möchte, findet den freiwilligen PayPal.me-Link auf der
[deutschen Projektwebsite](https://strugglechen1337.github.io/thor-stream-butler/de/#support).
