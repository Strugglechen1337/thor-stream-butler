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

## [0.7.0-alpha.1] - 2026-07-18

### English

#### Added

- Separate Wi-Fi and Ethernet streaming profiles: every tile can optionally
  store a dedicated Ethernet profile (resolution, FPS, bitrate) in addition
  to its default profile. The pre-launch check applies the Ethernet profile
  on wired connections and shows which profile is active; all other
  transports use the default profile. Tiles indicate a configured LAN
  profile, and configuration export/import carries it along.

#### Changed

- Room schema 3 adds the optional Ethernet profile columns with a tested
  migration; existing tiles keep their profile unchanged.

### Deutsch

#### Hinzugefügt

- Getrennte WLAN- und Ethernet-Streaming-Profile: Jede Kachel kann optional
  zusätzlich zum Standardprofil ein eigenes Ethernet-Profil (Auflösung, FPS,
  Bitrate) speichern. Der Netzwerkcheck vor dem Start wendet bei
  Kabelverbindung das Ethernet-Profil an und zeigt das aktive Profil; alle
  anderen Transporte nutzen das Standardprofil. Kacheln kennzeichnen ein
  gesetztes LAN-Profil, und der Konfigurations-Export/-Import nimmt es mit.

#### Geändert

- Room-Schema 3 ergänzt die optionalen Ethernet-Profilspalten mit getesteter
  Migration; bestehende Kacheln behalten ihr Profil unverändert.

## [0.6.0-alpha.1] - 2026-07-18

### English

#### Added

- Streaming session timer: launching a streaming app records a local session
  marker, and returning to the dashboard completes it. The last session
  (app, duration, start time) is shown on the dashboard. Sessions shorter
  than one minute or longer than twelve hours are discarded as implausible.
  Tracking is local only, permissionless, and an approximation because
  Android grants no app-usage insight without extra permissions.

### Deutsch

#### Hinzugefügt

- Streaming-Session-Timer: Beim Start einer Streaming-App wird lokal ein
  Session-Marker gesetzt, die Rückkehr zum Dashboard schließt ihn ab. Die
  letzte Session (App, Dauer, Startzeit) erscheint auf dem Dashboard.
  Sessions unter einer Minute oder über zwölf Stunden werden als
  unplausibel verworfen. Die Erfassung ist rein lokal, berechtigungsfrei
  und eine Näherung, da Android ohne Zusatzberechtigungen keinen Einblick
  in die App-Nutzung gewährt.

## [0.5.0-alpha.1] - 2026-07-18

### English

#### Added

- Quick Settings tile: shows the most recent stored network quality as its
  state and subtitle and opens the app directly into a running network test
  when tapped. The tile never measures on its own.
- Controller test (Settings → Appearance and input): visualizes pressed
  buttons, D-pad, both analog sticks, and trigger values live, and lists
  connected controllers. Gamepad input is captured only while the test is
  open; touch and the system back gesture always exit.
- Device status on the network test screen: battery level, charging state,
  and battery temperature refresh every ten seconds, with a notice when the
  battery gets hot.

#### Fixed

- Release signature verification now invokes `apksigner` from the exact Android
  Build Tools directory instead of relying on the runner's executable search path.

### Deutsch

#### Hinzugefügt

- Quick-Settings-Kachel: zeigt die zuletzt gespeicherte Netzwerkqualität als
  Status und Untertitel und öffnet beim Antippen die App direkt mit laufendem
  Netzwerktest. Die Kachel misst nie selbstständig.
- Controller-Test (Einstellungen → Darstellung und Bedienung): visualisiert
  gedrückte Tasten, Steuerkreuz, beide Analog-Sticks und Trigger-Werte live
  und listet verbundene Controller auf. Gamepad-Eingaben werden nur bei
  geöffnetem Test abgefangen; Touch und die System-Zurück-Geste beenden ihn
  jederzeit.
- Gerätestatus im Netzwerktest: Akkustand, Ladezustand und Akkutemperatur
  aktualisieren sich alle zehn Sekunden, mit Hinweis bei heißem Akku.

#### Behoben

- Die Release-Signaturprüfung ruft `apksigner` jetzt direkt aus dem Android-
  Build-Tools-Verzeichnis auf, statt sich auf den Suchpfad des Runners zu verlassen.

## [0.4.0-alpha.1] - 2026-07-18

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
- Full English and German privacy pages, an MIT license, bilingual third-party
  notices, in-app privacy details, and an adaptive Thor launcher icon.
- IPv6 host validation supports bracketed and scoped forms without DNS lookups.
- Bilingual physical-device validation and store/Data-safety documents define
  the remaining production release gate without exposing private test data.

#### Changed

- Pre-launch diagnostics now test the tile's linked host and expose Wake-on-LAN
  directly when that host is unreachable.
- Dashboard focus restoration, nested focus visibility, D-pad tile ordering,
  and removal confirmation improve controller use.
- Room schema 2 adds host assignments and streaming profiles with a tested
  migration that preserves existing tiles.
- Updates the build to Gradle 9.6.1, Kotlin 2.4.10, Compose BOM 2026.06.01,
  AndroidX Core 1.19.0, Hilt Navigation 1.4.0, and Coroutines 1.11.0.
- Configuration import is size-bounded, validates all references and values,
  replaces Room data transactionally, honors cancellation, and restores it if
  the settings write fails or the operation is cancelled.
- History is capped at the latest 100 measurements. Unexpected network, storage,
  discovery, and ViewModel failures now preserve cancellation and surface safely.
- Stored diagnostic targets, ping counts, and test durations are normalized when
  read or written. Unknown future Room enum values use non-crashing fallbacks.
- Quality evaluation now considers DNS, measured download speed, and Wi-Fi link
  rate in addition to latency, jitter, packet loss, transport, signal, and host reachability.
- Portrait handhelds use bottom navigation, system bars follow the selected theme,
  focused app-picker rows are visible, and destructive actions require confirmation.
- Partial diagnostic failures stay gray and can never trigger automatic launch;
  IPv6-only local addresses and gateways are displayed when IPv4 is unavailable.
- The signing helper keeps passwords out of `keytool` command arguments and
  verifies every individual GitHub secret upload.
- Release variants now use R8 and resource shrinking. CI builds APK and AAB,
  runs instrumentation tests on Android 9/API 28 and Android 15/API 35, pins
  all workflow actions to reviewed commits, rejects new moderate-or-higher
  dependency vulnerabilities, and the
  release workflow rejects unsigned bundles explicitly.
- Gradle now verifies the wrapper distribution and resolved dependency artifacts
  with reviewed SHA-256 metadata across Windows, Linux, and macOS build hosts.

#### Testing

- Added recommendation, comparable-history, dashboard launch-flow, Room
  migration, configuration transfer, persisted-state recovery, and private-log
  coverage.
- The final local run passed 40 JVM tests, 14 Android instrumentation tests,
  debug/release lint, an optimized APK/AAB build, D-pad navigation, permission
  denial, configuration, diagnostics, history, and real target-app launch on Android 15.

#### Migration

- Existing local data is migrated automatically from Room schema 1 to 2.
- Configuration import replaces apps, hosts, and settings. It replaces history
  only when the selected file contains history data.

#### Known limitations

- This is the first prerelease signed with the permanent production update key.
  It cannot update older debug-signed alphas in place; export the configuration
  if needed before uninstalling an older build and its local data.
- Android 9 and Android 15 emulator validation is complete; physical
  handheld/controller, vendor Wi-Fi, Ethernet, Wake-on-LAN, and long-session
  validation is still pending.
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
- Vollständige englische und deutsche Datenschutzseiten, eine MIT-Lizenz,
  zweisprachige Drittanbieterhinweise, Datenschutzdetails in der App und ein
  adaptives Thor-Launcher-Icon.
- Die IPv6-Hostvalidierung unterstützt Klammer- und Bereichsnotation ohne DNS-Abfrage.
- Zweisprachige Hardware-Abnahme- und Store-/Data-Safety-Dokumente definieren
  das verbleibende Produktions-Gate ohne private Testdaten zu veröffentlichen.

#### Geändert

- Der Netzwerkcheck vor dem Start prüft nun den verknüpften Host und bietet bei
  Nichterreichbarkeit direkt Wake-on-LAN an.
- Fokuswiederherstellung, sichtbarer Fokus innerhalb der Kachel,
  D-Pad-Sortierung und Löschbestätigung verbessern die Controller-Bedienung.
- Room-Schema 2 ergänzt Host-Zuordnungen und Streaming-Profile mit einer
  getesteten Migration, die vorhandene Kacheln erhält.
- Aktualisiert den Build auf Gradle 9.6.1, Kotlin 2.4.10, Compose BOM 2026.06.01,
  AndroidX Core 1.19.0, Hilt Navigation 1.4.0 und Coroutines 1.11.0.
- Der Konfigurationsimport ist größenbegrenzt, validiert Referenzen und Werte,
  ersetzt Room-Daten transaktional, respektiert Cancellation und stellt sie bei
  einem Einstellungsfehler oder Abbruch wieder her.
- Die Historie ist auf die letzten 100 Messungen begrenzt. Unerwartete Netzwerk-,
  Speicher-, Such- und ViewModel-Fehler werden ohne Verlust der Cancellation sicher angezeigt.
- Gespeicherte Diagnoseziele, Ping-Anzahlen und Testdauern werden beim Lesen und
  Schreiben normalisiert. Unbekannte zukünftige Room-Enum-Werte nutzen
  absturzsichere Rückfallwerte.
- Die Bewertung berücksichtigt nun zusätzlich DNS, gemessene Downloadrate und
  WLAN-Linkrate sowie weiterhin Latenz, Jitter, Paketverlust, Transport, Signal und Host.
- Portrait-Handhelds verwenden die untere Navigation, Systemleisten folgen dem
  Theme, fokussierte App-Zeilen sind sichtbar und destruktive Aktionen verlangen Bestätigung.
- Teilweise fehlgeschlagene Diagnosen bleiben grau und können keinen automatischen
  App-Start auslösen; IPv6-Adressen und -Gateways werden ohne IPv4 angezeigt.
- Der Signing-Helfer hält Passwörter aus `keytool`-Kommandoargumenten heraus und
  prüft jeden einzelnen Upload eines GitHub-Secrets.
- Release-Varianten verwenden R8 und Ressourcenverkleinerung. CI baut APK und AAB,
  führt Instrumentationstests auf Android 9/API 28 und Android 15/API 35 aus und
  setzt alle Workflow-Aktionen auf geprüfte Commits fest, blockiert neue
  Abhängigkeitsschwachstellen ab mittlerer
  Schwere und weist unsignierte Bundles ausdrücklich zurück.
- Gradle prüft Wrapper-Distribution und aufgelöste Abhängigkeitsartefakte jetzt
  per kontrollierter SHA-256-Metadaten auf Windows-, Linux- und macOS-Buildhosts.

#### Tests

- Neue Abdeckung für Profilempfehlungen, vergleichbare Historientrends,
  Dashboard-Startablauf, Room-Migration, Konfigurationstransfer,
  Wiederherstellung gespeicherter Daten und privates Log.
- Der abschließende lokale Lauf bestand 40 JVM-Tests, 14 Android-
  Instrumentationstests, Debug-/Release-Lint, optimierten APK-/AAB-Build,
  D-Pad-Navigation, Berechtigungsverweigerung, Konfiguration, Diagnose, Historie
  und den echten Ziel-App-Start auf Android 15.

#### Migration

- Vorhandene lokale Daten werden automatisch von Room-Schema 1 auf 2 migriert.
- Ein Konfigurationsimport ersetzt Apps, Hosts und Einstellungen. Die Historie
  wird nur ersetzt, wenn die ausgewählte Datei Historiendaten enthält.

#### Bekannte Einschränkungen

- Dies ist das erste Prerelease mit dem dauerhaften produktiven Update-Schlüssel.
  Es kann ältere debug-signierte Alphas nicht direkt aktualisieren; exportiere
  bei Bedarf die Konfiguration, bevor du einen alten Build samt lokalen Daten deinstallierst.
- Die Emulatorprüfung auf Android 9 und Android 15 ist abgeschlossen; Tests auf
  physischem Handheld und Controller sowie herstellerspezifischem WLAN, Ethernet,
  Wake-on-LAN und langen Sitzungen stehen noch aus.
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
