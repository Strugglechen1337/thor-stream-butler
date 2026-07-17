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
