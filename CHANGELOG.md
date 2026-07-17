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
