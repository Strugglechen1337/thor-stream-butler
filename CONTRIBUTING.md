# Mitwirken an Thor Stream Butler

Danke für dein Interesse. Beiträge sollen den lokalen, controller-first Ansatz des Projekts erhalten.

## Vor einem Beitrag

1. Suche in den Issues nach bestehenden Diskussionen.
2. Nutze für größere Änderungen zunächst ein Feature-Issue.
3. Veröffentliche niemals IP-Adressen, SSIDs, MAC-Adressen oder andere Netzwerkdaten.

## Entwicklungsumgebung

- JDK 17
- Android SDK 37
- Android Build Tools 36.0.0 oder neuer
- Android Studio oder Gradle Wrapper

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Instrumentierte Room-Tests werden mit `:app:assembleDebugAndroidTest` gebaut und auf einem Emulator oder Gerät mit `connectedDebugAndroidTest` ausgeführt.

## Architekturregeln

- Bewertungs- und Validierungslogik bleibt unabhängig von Android-Komponenten.
- Features greifen über Repository- und Service-Interfaces auf Daten und Netzwerk zu.
- Nicht messbare Werte bleiben `null`; niemals plausible Messwerte simulieren.
- Netzwerkoperationen benötigen Timeouts und sauberes Coroutine-Cancellation-Handling.
- Gefährliche Berechtigungen werden erst beim Auslösen der zugehörigen Funktion angefragt.
- Neue UI muss in Portrait und Landscape sowie mit D-Pad/Controller bedienbar sein.
- Keine Tracker, Werbung, Telemetrie oder unnötigen Berechtigungen.

## Pull Requests

- Halte Änderungen fokussiert und Commits nachvollziehbar.
- Ergänze Tests für neue Kernlogik und Fehlerfälle.
- Beschreibe sichtbare Änderungen und füge bereinigte Screenshots hinzu.
- Führe mindestens Unit-Tests, Lint und Debug-Build aus.

Markennamen dürfen zur Beschreibung unterstützter Dienste verwendet werden. Geschützte Logos oder andere Assets dürfen nur mit nachgewiesener Nutzungsberechtigung aufgenommen werden.

