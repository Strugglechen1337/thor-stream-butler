# Changelog / Änderungsprotokoll

All notable changes are documented in both English and German. Entries in both
language sections must remain equivalent.

Alle wichtigen Änderungen werden auf Englisch und Deutsch dokumentiert. Die
Einträge beider Sprachabschnitte müssen inhaltlich gleichwertig bleiben.

## [Unreleased]

### English

No unreleased changes.

### Deutsch

Keine unveröffentlichten Änderungen.

## [0.4.0-alpha.1] - 2026-07-17

### English

#### Added

- Dashboard tiles can be linked to a saved local host and store an individual
  resolution, frame-rate, and bitrate profile.
- Network-aware profile recommendations consider latency, jitter, packet loss,
  transport, host reachability, and measured download speed.
- User-initiated Sunshine/Moonlight-compatible host discovery uses Android NSD
  for `_nvstream._tcp` without subnet scanning or hidden APIs.
- Versioned local JSON export/import covers apps, hosts, settings, and optional
  measurement history through Android's document picker.
- History filters, averages, a compact latency sparkline, and comparisons only
  between matching connection contexts.
- An optional privacy-safe diagnostic event log stores at most 200 timestamps
  and predefined event types, never network identifiers.
- A bilingual PowerShell helper prepares a permanent production signing key and
  can explicitly configure all four GitHub signing secrets.

#### Changed

- Pre-launch diagnostics now test the tile's linked host and expose Wake-on-LAN
  directly when that host is unreachable.
- Dashboard focus restoration, nested focus visibility, D-pad tile ordering,
  and removal confirmation improve controller use.
- Room schema 2 adds host assignments and streaming profiles with a tested
  migration that preserves existing tiles.
- Updates the build to Gradle 9.6.1, Kotlin 2.4.10, Compose BOM 2026.06.01,
  AndroidX Core 1.19.0, Hilt Navigation 1.4.0, and Coroutines 1.11.0.

#### Testing

- Added recommendation, comparable-history, dashboard launch-flow, Room
  migration, configuration transfer, and private-log coverage.

#### Migration

- Existing local data is migrated automatically from Room schema 1 to 2.
- Configuration import replaces apps, hosts, and settings. It replaces history
  only when the selected file contains history data.

#### Known limitations

- The release remains a debug-signed alpha unless all production-signing
  secrets are configured. A debug-signed build may require uninstalling the
  previous alpha, which removes its local data.
- Physical Android handheld validation was not available during development.
- Discovery only finds compatible hosts advertising `_nvstream._tcp` on the
  same local network; routers and Android devices may filter discovery traffic.
- Stored profiles and recommendations are guidance. Thor Stream Butler cannot
  configure third-party streaming apps that expose no supported settings API.

### Deutsch

#### Hinzugefügt

- Dashboard-Kacheln können mit einem gespeicherten lokalen Host verknüpft werden
  und ein eigenes Auflösungs-, Bildraten- und Bitratenprofil speichern.
- Netzwerkabhängige Profilempfehlungen berücksichtigen Latenz, Jitter,
  Paketverlust, Transport, Host-Erreichbarkeit und gemessene Downloadrate.
- Die benutzergesteuerte Sunshine-/Moonlight-kompatible Host-Suche verwendet
  Android NSD für `_nvstream._tcp` ohne Subnetz-Scan oder versteckte APIs.
- Versionierter lokaler JSON-Export/-Import umfasst Apps, Hosts, Einstellungen
  und optional die Messhistorie über die Android-Dateiauswahl.
- Historienfilter, Mittelwerte, ein kompakter Latenzverlauf und Vergleiche nur
  zwischen passenden Verbindungskontexten.
- Ein optionales datenschutzsicheres Diagnoseprotokoll speichert höchstens 200
  Zeitpunkte und fest definierte Ereignistypen, niemals Netzwerkkennungen.
- Ein zweisprachiger PowerShell-Helfer bereitet einen dauerhaften produktiven
  Signing-Schlüssel vor und kann explizit alle vier GitHub-Secrets einrichten.

#### Geändert

- Der Netzwerkcheck vor dem Start prüft nun den verknüpften Host und bietet bei
  Nichterreichbarkeit direkt Wake-on-LAN an.
- Fokuswiederherstellung, sichtbarer Fokus innerhalb der Kachel,
  D-Pad-Sortierung und Löschbestätigung verbessern die Controller-Bedienung.
- Room-Schema 2 ergänzt Host-Zuordnungen und Streaming-Profile mit einer
  getesteten Migration, die vorhandene Kacheln erhält.
- Aktualisiert den Build auf Gradle 9.6.1, Kotlin 2.4.10, Compose BOM 2026.06.01,
  AndroidX Core 1.19.0, Hilt Navigation 1.4.0 und Coroutines 1.11.0.

#### Tests

- Neue Abdeckung für Profilempfehlungen, vergleichbare Historientrends,
  Dashboard-Startablauf, Room-Migration, Konfigurationstransfer und privates Log.

#### Migration

- Vorhandene lokale Daten werden automatisch von Room-Schema 1 auf 2 migriert.
- Ein Konfigurationsimport ersetzt Apps, Hosts und Einstellungen. Die Historie
  wird nur ersetzt, wenn die ausgewählte Datei Historiendaten enthält.

#### Bekannte Einschränkungen

- Das Release bleibt eine debug-signierte Alpha, solange nicht alle produktiven
  Signing-Secrets eingerichtet sind. Ein Debug-Build kann die Deinstallation
  der vorherigen Alpha erfordern; dabei werden deren lokale Daten gelöscht.
- Eine Prüfung auf physischer Android-Handheld-Hardware war nicht verfügbar.
- Die Suche findet nur kompatible Hosts, die `_nvstream._tcp` im selben lokalen
  Netz ankündigen; Router und Android-Geräte können Discovery-Verkehr filtern.
- Gespeicherte Profile und Empfehlungen dienen als Orientierung. Thor Stream
  Butler kann Drittanbieter-Apps ohne unterstützte Einstellungs-API nicht konfigurieren.

## [0.3.0-alpha.1] - 2026-07-17

### English

#### Added

- VPN-aware diagnostics: active VPN transports are detected before their
  underlying Wi-Fi, Ethernet, or cellular transport and shown explicitly.
- Active VPN connections receive a non-critical quality warning with an
  actionable recommendation to compare routing when streaming is unstable.
- Safe automated prerelease fallback: tags containing a hyphen can publish an
  explicitly named debug APK when no production-signing secrets are configured.

#### Changed

- The Release workflow now validates complete bilingual notes, derives the
  bilingual release title, marks prereleases correctly, and blocks stable tags
  unless all production-signing secrets are available.

#### Known limitations

- The downloadable APK is debug-signed and cannot reliably update another
  alpha build. Uninstall an older alpha first, which removes its local settings,
  configured hosts, and measurement history.
- Installation on physical handheld hardware was not available during this
  release run.
- Automatic Sunshine and Moonlight host discovery is not implemented yet.

### Deutsch

#### Hinzugefügt

- VPN-bewusste Diagnose: Aktive VPN-Transporte werden vor dem darunterliegenden
  WLAN-, Ethernet- oder Mobilfunktransport erkannt und eindeutig angezeigt.
- Aktive VPN-Verbindungen erhalten einen nicht kritischen Qualitätshinweis mit
  einer konkreten Empfehlung, bei instabilem Streaming die Route zu vergleichen.
- Sicherer automatischer Prerelease-Fallback: Tags mit Bindestrich dürfen eine
  eindeutig benannte Debug-APK veröffentlichen, wenn keine produktiven
  Signing-Secrets konfiguriert sind.

#### Geändert

- Der Release-Workflow prüft jetzt vollständige zweisprachige Release Notes,
  übernimmt daraus den zweisprachigen Titel, kennzeichnet Prereleases korrekt
  und blockiert stabile Tags ohne alle produktiven Signing-Secrets.

#### Bekannte Einschränkungen

- Die herunterladbare APK ist debug-signiert und kann andere Alpha-Builds nicht
  zuverlässig aktualisieren. Deinstalliere eine ältere Alpha zuerst; dabei
  werden deren lokale Einstellungen, konfigurierte Hosts und Messhistorie entfernt.
- Eine Installation auf physischer Handheld-Hardware stand für diesen
  Release-Lauf nicht zur Verfügung.
- Automatische Sunshine- und Moonlight-Host-Erkennung ist noch nicht umgesetzt.

## [0.2.0-alpha.1] - 2026-07-17

### English

#### Added

- Full bilingual in-app interface: English is the new default language, German
  is bundled and follows the system language or the per-app language setting
  (Android 13+). All screens, dialogs, error messages, diagnostic steps, and
  quality assessments are localized.
- Release signing infrastructure: local builds read `signing/keystore.properties`
  (gitignored), and a new `Release` GitHub Actions workflow builds a signed APK
  with SHA-256 checksum for `v*` tags using repository secrets.

#### Changed

- Number formatting in measurements now follows the active locale instead of
  always using German decimal formatting.
- Diagnostic progress steps are locale-independent identifiers internally,
  which keeps stored data and tests independent of the display language.

#### Fixed

- A failed pre-launch network check no longer stores an invented "no
  connection" measurement in the history; the error is now shown directly in
  the launch dialog instead.
- Installed-app lookups for dashboard tiles no longer run on the main thread.

#### Known limitations

- The downloadable APK is still a debug-signed alpha preview. It cannot update
  v0.1.0-alpha.1 in place; uninstall the previous alpha first, which removes
  its local settings, hosts, and measurement history.
- Installation on physical handheld hardware was not available during this
  release run.
- Automatic Sunshine and Moonlight host discovery is not implemented yet.

### Deutsch

#### Hinzugefügt

- Vollständig zweisprachige App-Oberfläche: Englisch ist die neue
  Standardsprache, Deutsch ist enthalten und folgt der Systemsprache bzw. der
  App-Sprach-Einstellung (Android 13+). Alle Screens, Dialoge, Fehlermeldungen,
  Diagnoseschritte und Qualitätsbewertungen sind lokalisiert.
- Release-Signing-Infrastruktur: Lokale Builds lesen
  `signing/keystore.properties` (gitignored), und ein neuer
  `Release`-GitHub-Actions-Workflow baut für `v*`-Tags eine signierte APK mit
  SHA-256-Prüfsumme aus Repository-Secrets.

#### Geändert

- Zahlenformatierung in Messwerten folgt jetzt der aktiven Sprache statt immer
  der deutschen Dezimalformatierung.
- Diagnose-Fortschrittsschritte sind intern sprachunabhängige Kennungen; damit
  bleiben gespeicherte Daten und Tests von der Anzeigesprache unabhängig.

#### Behoben

- Ein fehlgeschlagener Netzwerkcheck vor dem App-Start speichert keine
  erfundene "keine Verbindung"-Messung mehr in der Historie; der Fehler wird
  stattdessen direkt im Start-Dialog angezeigt.
- Die Abfrage installierter Apps für Dashboard-Kacheln läuft nicht mehr auf dem
  Main-Thread.

#### Bekannte Einschränkungen

- Die herunterladbare APK ist weiterhin eine debug-signierte Alpha-Vorschau.
  Sie kann v0.1.0-alpha.1 nicht direkt aktualisieren; deinstalliere die
  vorherige Alpha zuerst, wodurch deren lokale Einstellungen, Hosts und
  Messhistorie entfernt werden.
- Eine Installation auf physischer Handheld-Hardware stand für diesen
  Release-Lauf nicht zur Verfügung.
- Automatische Sunshine- und Moonlight-Host-Erkennung ist noch nicht umgesetzt.

## [0.1.0-alpha.1] - 2026-07-17

### English

#### Added

- First installable development preview of the controller-first Android launcher.
- Local dashboard entries for installed streaming apps and custom Android apps.
- Optional launch protection with network diagnostics and a green, yellow, red,
  or gray quality rating.
- Ping, jitter, packet-loss, connection-type, Wi-Fi, DNS, internet, and local-host
  measurements where Android makes the data available.
- Local host management, TCP checks, and Wake-on-LAN magic packets.
- Local Room history, DataStore settings, controller navigation, and responsive
  portrait and landscape layouts.
- Responsive project website and public project documentation in English and German.

#### Known limitations

- The downloadable APK is a debug-signed alpha preview, not a production-signed
  stable release. A future production-signed version cannot update it directly;
  uninstall this preview first, which removes its local settings, hosts, and history.
- The in-app interface is currently German only. GitHub documentation, the project
  website, and release information are available in English and German.
- The APK build, package metadata, and signature were verified, but installation on
  physical handheld hardware was not available during this release run.
- Automatic Sunshine and Moonlight host discovery is not implemented yet.
- Project website screenshots are sanitized interface previews rather than captures
  from tested handheld hardware.

### Deutsch

#### Hinzugefügt

- Erste installierbare Entwicklungsvorschau des controller-first Android-Launchers.
- Lokale Dashboard-Einträge für installierte Streaming-Apps und frei gewählte Android-Apps.
- Optionaler Startschutz mit Netzwerkdiagnose und Qualitätsbewertung in Grün, Gelb,
  Rot oder Grau.
- Messungen für Ping, Jitter, Paketverlust, Verbindungstyp, WLAN, DNS, Internet und
  lokale Hosts, soweit Android die Daten bereitstellt.
- Lokale Hostverwaltung, TCP-Prüfungen und Wake-on-LAN-Magic-Packets.
- Lokale Room-Historie, DataStore-Einstellungen, Controller-Navigation sowie responsive
  Portrait- und Landscape-Layouts.
- Responsive Projektwebsite und öffentliche Projektdokumentation auf Englisch und Deutsch.

#### Bekannte Einschränkungen

- Die herunterladbare APK ist eine debug-signierte Alpha-Vorschau und kein
  produktiv signiertes stabiles Release. Eine zukünftige produktiv signierte Version
  kann sie nicht direkt aktualisieren; diese Vorschau muss zuerst deinstalliert werden,
  wodurch lokale Einstellungen, Hosts und die Historie entfernt werden.
- Die Oberfläche in der App ist derzeit nur auf Deutsch verfügbar. GitHub-Dokumentation,
  Projektwebsite und Release-Informationen gibt es auf Englisch und Deutsch.
- APK-Build, Paketmetadaten und Signatur wurden geprüft; eine Installation auf echter
  Handheld-Hardware war während dieses Release-Durchlaufs jedoch nicht möglich.
- Die automatische Sunshine- und Moonlight-Host-Erkennung ist noch nicht implementiert.
- Die Screenshots der Projektwebsite sind bereinigte Oberflächenvorschauen und keine
  Aufnahmen von getesteter Handheld-Hardware.
