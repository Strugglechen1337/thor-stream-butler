# Thor Stream Butler

Thor Stream Butler ist ein lokaler Gaming-Streaming-Launcher für Android-Handhelds wie AYN Thor/Odin, Retroid Pocket und vergleichbare Geräte. Die App bündelt installierte Streaming-Apps, prüft auf Wunsch vor dem Start die Netzwerkqualität und verwaltet lokale Streaming-Hosts einschließlich Wake-on-LAN.

Der aktuelle Stand ist ein kompilierbarer MVP ohne Konto, Cloud-Backend, Werbung, Tracking oder Telemetrie.

## Funktionsumfang

- Adaptive Launcher-Oberfläche für Portrait und Landscape
- Bedienung per Controller, D-Pad, Touch und Tastatur
- Große fokussierbare Kacheln mit deutlich sichtbarem Fokusrahmen
- Auswahl launchbarer Android-Apps über den Package Manager
- Lokale Speicherung von Name, Package Name, Kategorie, Icon-Referenz und Sortierung
- Prüfung des Launch-Intents und verständliche Fehlermeldung bei fehlenden Apps
- Optionaler Netzwerkcheck vor dem App-Start
- Grün/Gelb/Rot/Grau-Bewertung mit Begründung und Empfehlungen
- Vollständiger, abbrechbarer Netzwerktest mit Live-Fortschritt
- Lokale Hostverwaltung, TCP-Port-Test und Wake-on-LAN
- Room-Historie mit einfachem Latenztrend zur vorherigen Messung
- DataStore-Einstellungen für Startablauf, Diagnose und Darstellung

Unterstützte Kategorien:

- GeForce NOW
- Xbox Cloud Gaming
- Xbox Remote Play / XBPlay
- PlayStation Remote Play / PXPlay
- Moonlight
- Steam Link
- Sunshine Host
- frei konfigurierbare Android-Apps

Markennamen dienen ausschließlich zur Kategorisierung. Das Projekt enthält keine geschützten Dienstlogos; installierte App-Icons werden zur Laufzeit lokal über Android geladen.

## Screenshots

Die folgenden Grafiken sind Platzhalter und werden nach Tests auf echten Handhelds durch Gerätescreenshots ersetzt.

| Landscape-Dashboard | Portrait-Netzwerktest |
|---|---|
| ![Dashboard-Platzhalter](docs/screenshots/dashboard-landscape.svg) | ![Netzwerktest-Platzhalter](docs/screenshots/networktest-portrait.svg) |

## Technische Basis

- Kotlin mit Coroutines und Flow
- Jetpack Compose und Material 3
- Gradle Kotlin DSL und Version Catalog
- Minimum SDK 28
- Compile/Target SDK 37 (Android 17)
- Android Gradle Plugin 9.3.0 mit eingebautem Kotlin
- MVVM und Repository Pattern
- Hilt und KSP
- Room für Launcher-Einträge, Hosts und Messhistorie
- Preferences DataStore für Einstellungen
- Navigation Compose

WorkManager wird im MVP bewusst nicht eingebunden: Es gibt derzeit keine zuverlässige, nutzerrelevante Hintergrundaufgabe. Diagnose und App-Start sind explizite, sichtbare Benutzeraktionen.

## Projektstruktur

Der MVP verwendet ein einzelnes `app`-Modul. Die Paketgrenzen sind so gewählt, dass sie später ohne große Umbauten in Gradle-Module überführt werden können.

```text
app/src/main/java/de/thorstream/butler/
├── core/
│   ├── common/          Result- und Fehlertypen
│   ├── designsystem/    Thor-Theme und Farben
│   ├── network/         Berechnungen, Bewertung, WOL-Paket
│   └── validation/      Host-, IPv4- und MAC-Validierung
├── data/
│   ├── database/        Room Entities, DAOs und Datenbank
│   ├── datastore/       Einstellungen
│   ├── di/              Hilt-Module
│   ├── repository/      Android- und Room-Repositories
│   └── service/         Netzwerk-, Ping-, Host- und WOL-Dienste
├── domain/
│   ├── model/           Android-unabhängige Datenmodelle
│   ├── repository/      Repository-Interfaces
│   └── service/         Service-Interfaces
├── feature/
│   ├── dashboard/
│   ├── history/
│   ├── hosts/
│   ├── networktest/
│   └── settings/
└── navigation/          Adaptive App-Navigation
```

Zentrale Schnittstellen sind `NetworkDiagnosticsService`, `PingService`, `SpeedTestService`, `HostDiscoveryService`, `WakeOnLanService`, `InstalledAppsRepository`, `StreamingEntryRepository` und `NetworkHistoryRepository`.

## Build

### Voraussetzungen

- JDK 17
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0 oder neuer
- Android Studio mit Unterstützung für Android 17 oder eine Kommandozeileninstallation des SDK

Das Projekt enthält den Gradle Wrapper 9.5.0. Ein global installiertes Gradle ist nicht erforderlich.

### Lokale SDK-Konfiguration

Lege eine nicht versionierte `local.properties` im Projektverzeichnis an:

```properties
sdk.dir=C\:\\Users\\NAME\\AppData\\Local\\Android\\Sdk
```

Unter macOS/Linux beispielsweise:

```properties
sdk.dir=/Users/NAME/Library/Android/sdk
```

### Kompilieren und testen

Windows PowerShell:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebugAndroidTest
./gradlew.bat :app:lintDebug
```

macOS/Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:lintDebug
```

Die Debug-APK liegt anschließend unter `app/build/outputs/apk/debug/app-debug.apk`.

Die JVM-Tests laufen ohne Gerät. Die Room-Repositorytests werden als instrumentierte Tests gebaut und können auf einem Emulator oder Gerät mit `connectedDebugAndroidTest` ausgeführt werden.

## Netzwerkdiagnose

Der Netzwerktest liest ausschließlich öffentliche Android-APIs und führt reale Messversuche aus. Nicht verfügbare Werte bleiben `null` und werden in der Oberfläche als „Nicht verfügbar“ oder „Nicht messbar“ angezeigt.

Erfasste Werte:

- aktiver Transport: Ethernet, WLAN, Mobilfunk, VPN oder anderer Transport
- lokale IPv4-Adresse und Standard-Gateway, soweit Android sie bereitstellt
- SSID, WLAN-Frequenz, Link-Geschwindigkeit und Signalstärke, soweit Berechtigungen und Herstellerimplementierung dies erlauben
- Androids validierter Internetstatus
- DNS-Auflösung
- Ping-Latenz, Jitter und Paketverlust
- optionaler kurzer HTTPS-Downloadtest
- Erreichbarkeit eines explizit konfigurierten Hosts beziehungsweise TCP-Ports

Für Ping wird zunächst das auf Android übliche `/system/bin/ping` mit getrennten Prozessargumenten verwendet. Wenn das Systemprogramm auf einem Gerät nicht verfügbar ist, fällt der Dienst auf `InetAddress.isReachable()` zurück. Manche Netze oder Geräte blockieren ICMP; das ist kein App-Absturz, sondern führt zu Paketverlust oder einem nicht messbaren Ergebnis.

Jitter ist der Mittelwert der absoluten Differenzen aufeinanderfolgender erfolgreicher Latenzmessungen. Paketverlust ist `(gesendet - empfangen) / gesendet × 100`.

Der optionale Downloadtest ist standardmäßig deaktiviert. Wenn der Benutzer ihn aktiviert, lädt die App zeitlich begrenzt Testdaten per HTTPS von `speed.cloudflare.com`. Dabei sieht der Testanbieter technisch die öffentliche IP-Adresse. Die App sendet keine Kontodaten und speichert keine Antwortinhalte.

## Qualitätsbewertung

Die Bewertungslogik liegt vollständig in `core/network/QualityEvaluator.kt` und hängt nicht von Android-Komponenten ab. Die Grenzwerte sind zentral in `QualityThresholds` definiert.

Aktuelle Standardwerte:

| Messwert | Optimal/gut | Warnung | Problematisch |
|---|---:|---:|---:|
| Latenz | bis 30 ms | über 30 ms | über 60 ms |
| Jitter | bis 10 ms | über 10 ms | über 20 ms |
| Paketverlust | 0 % | über 0 % | über 1 % |
| WLAN-Signal | ab 55 % | unter 55 % | unter 35 % |

Zusätzlich berücksichtigt die Bewertung Transporttyp, 2,4-/5-/6-GHz-WLAN, validierten Internetzugang und Host-Erreichbarkeit. Downloadgeschwindigkeit allein entscheidet nie über die Gesamtqualität. Ethernet sowie 5-/6-GHz-WLAN werden bevorzugt, Mobilfunk und 2,4-GHz-WLAN führen bei ansonsten guten Messwerten zu einem Hinweis.

## Startablauf

1. Der Benutzer fokussiert und aktiviert eine Kachel.
2. Falls aktiviert, startet ein kurzer Netzwerkcheck ohne Downloadtest.
3. Die App zeigt Bewertung und Begründung.
4. Grün startet nach kurzer Anzeige automatisch, sofern diese Option aktiviert ist.
5. Gelb zeigt einen Hinweis und startet anschließend.
6. Rot bietet „Trotzdem starten“, „Erneut testen“ und „Abbrechen“; die Rückfrage kann deaktiviert werden.
7. Nicht messbare Ergebnisse werden grau dargestellt und erfordern eine bewusste Entscheidung.

## Berechtigungen

Die App fordert gefährliche Berechtigungen erst bei Nutzung der zugehörigen Funktion an.

| Berechtigung | Android-Version | Zweck | Verhalten ohne Berechtigung |
|---|---|---|---|
| `INTERNET` | alle | DNS, Ping/Fallback, TCP-Test, HTTPS-Test, Wake-on-LAN | Netzwerkdiagnose und Hostfunktionen nicht möglich |
| `ACCESS_NETWORK_STATE` | alle | aktives Netzwerk, Transport und validierter Internetstatus | Basisdiagnose nicht möglich |
| `ACCESS_WIFI_STATE` | alle | WLAN-Linkdaten | WLAN-Details fehlen |
| `ACCESS_COARSE_LOCATION` + `ACCESS_FINE_LOCATION` | bis Android 12L, Manifest `maxSdkVersion=32` | SSID/WLAN-Details auf älteren Versionen; Android 12 verlangt die gemeinsame Anfrage | SSID und einzelne WLAN-Werte fehlen |
| `NEARBY_WIFI_DEVICES` | ab Android 13 | aktuelle WLAN-Details; mit `neverForLocation` | SSID und einzelne WLAN-Werte fehlen |
| `ACCESS_LOCAL_NETWORK` | ab Android 17 bei Target SDK 37 | explizite TCP/UDP-Kommunikation mit gespeicherten LAN-Hosts und Wake-on-LAN | Host-Test und Wake-on-LAN werden nicht ausgeführt; Internettests bleiben möglich |

`CHANGE_WIFI_MULTICAST_STATE` wird im MVP nicht deklariert, da keine Multicast-/mDNS-Erkennung stattfindet. UDP-Broadcast für ein explizit konfiguriertes Wake-on-LAN-Ziel benötigt diese Berechtigung nicht. Sobald automatische Host-Erkennung implementiert wird, muss der konkrete Discovery-Ansatz erneut geprüft werden.

Die `<queries>`-Deklaration im Manifest ist keine Laufzeitberechtigung. Sie beschränkt die Package-Sichtbarkeit auf Activities mit Launcher-Intent.

Weiterführend: [Android Local Network Permission](https://developer.android.com/privacy-and-security/local-network-permission), [Android Wi-Fi Permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions).

## Datenschutz und Sicherheit

- kein Konto und kein Cloud-Backend
- keine Werbung, Tracker, Telemetrie oder Analytics-SDKs
- keine gespeicherten Zugangsdaten
- keine versteckten Android-APIs und kein Root-Zugriff
- Hosts, IP-/Netzwerkdaten, Einstellungen und Messhistorie bleiben in der privaten App-Sandbox
- Android-Backup ist für den MVP deaktiviert (`allowBackup=false`)
- Cleartext-HTTP ist deaktiviert; der optionale Downloadtest verwendet HTTPS
- Logs enthalten im MVP keine IP-Adressen, SSIDs oder MAC-Adressen
- Netzwerkoperationen haben Timeouts und respektieren Coroutine-Cancellation

Beim Deinstallieren entfernt Android die lokale App-Datenbank und DataStore-Einstellungen. Innerhalb der App kann die Messhistorie separat gelöscht werden.

## Bekannte Android-Einschränkungen

- SSID und WLAN-Metadaten können trotz Berechtigung durch Android oder den Gerätehersteller redigiert werden.
- Die WLAN-Link-Geschwindigkeit ist ein ausgehandelter PHY-Linkwert und nicht mit echter Downloadrate gleichzusetzen.
- Ein ICMP-blockierendes Ziel kann wie Paketverlust aussehen. Der Fallback ist geräteabhängig.
- `NET_CAPABILITY_VALIDATED` ist Androids Sicht auf Internetzugang, keine Garantie für die Verfügbarkeit eines konkreten Streaming-Dienstes.
- Wake-on-LAN funktioniert nur, wenn Host, Firmware, Netzwerkkarte und Router Broadcast/Magic Packets unterstützen.
- Der Standard-Broadcast `255.255.255.255` funktioniert nicht in jedem Netz; pro Host kann eine Subnetz-Broadcast- oder Unicast-Zieladresse eingetragen werden.
- Demo-Kacheln nutzen bekannte Package-Namen. Hersteller- oder Store-Varianten können abweichen und werden dann als nicht installiert angezeigt.
- Die Historie ist im MVP eine Liste der letzten 100 Messungen. Diagramme sind noch nicht umgesetzt.
- Automatische Sunshine-/Moonlight-Erkennung und Port-Scanning sind ausdrücklich nicht Bestandteil des MVP.

## Tests

Abgedeckt sind:

- Qualitätsbewertung einschließlich Grün/Gelb/Rot/Grau
- Jitter- und Paketverlustberechnung
- IPv4-, Hostname- und MAC-Validierung
- exakter Aufbau des 102-Byte-Wake-on-LAN-Pakets
- erfolgreiche und fehlgeschlagene Netzwerktest-ViewModel-Flows
- Fehlerfall ohne aktives Netzwerk, ohne erfundene Messwerte
- Room-Repositories für Kacheln, Hosts und Historie

Test-Fakes implementieren die Netzwerk-Service- und Einstellungsinterfaces ohne Android-Netzwerkzugriff.

## Roadmap nach dem MVP

- automatische Erkennung von Sunshine- und Moonlight-Hosts über einen Android-17-konformen, datenschutzfreundlichen Ablauf
- Port-Scanning nur für explizit angegebene Hosts
- Profile pro Streaming-Dienst
- empfohlene Auflösung und Bitrate
- unterschiedliche Profile für WLAN und Ethernet
- Widgets und Quick Settings Tile
- Export und Import der Konfiguration
- Backup auf ein lokales NAS
- Controller-Mapping-Test
- Streaming-Session-Timer
- Akku- und Temperaturüberwachung
- Erkennung von VPN-Verbindungen in Bewertung und UI
- Vergleich mehrerer WLAN-Netze
- Benachrichtigung bei instabiler Verbindung
- optionaler Companion-Dienst für Windows
