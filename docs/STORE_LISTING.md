# Store listing and data-safety draft / Store-Eintrag und Data-Safety-Entwurf

This document is release-facing and must stay equivalent in English and German.
It is a prepared draft, not permission to publish an unsigned or unverified build.

Dieses Dokument ist releasebezogen und muss auf Englisch und Deutsch inhaltlich
gleich bleiben. Es ist ein vorbereiteter Entwurf und keine Freigabe für einen
unsignierten oder ungeprüften Build.

## English

### Product metadata

- **App name:** Thor Stream Butler
- **Category:** Tools
- **Default language:** English (United States)
- **Package name:** `de.thorstream.butler`
- **Website:** <https://strugglechen1337.github.io/thor-stream-butler/>
- **Privacy policy:** <https://strugglechen1337.github.io/thor-stream-butler/privacy/>
- **Support:** <https://github.com/Strugglechen1337/thor-stream-butler/issues>

### Short description

Launch game streams with controller-first network checks and local hosts.

### Full description

Thor Stream Butler is a local-first gaming-stream launcher for Android
handhelds. It brings installed streaming apps, local hosts, network checks, and
Wake-on-LAN into one controller-friendly interface.

Use large focusable tiles with D-pad, controller, or touch. Before opening a
streaming app, Thor Stream Butler can evaluate latency, jitter, packet loss,
DNS, Wi-Fi quality, measured download speed, and an assigned local host. Clear
green, yellow, red, and gray results explain what was measured and what can be
improved.

Key capabilities:

- add launchable Android apps without embedding third-party brand assets;
- link tiles to Sunshine, Moonlight, Xbox, PlayStation, Steam Link, or custom hosts;
- test TCP reachability and send Wake-on-LAN magic packets;
- save local streaming profiles and receive conservative recommendations;
- review up to 100 recent measurements and comparable connection trends;
- export or import the configuration through Android's document picker;
- use English or German in portrait and landscape.

No account, advertising, tracking, analytics, telemetry, or cloud backend is
included. Network and host data remains in the app's private local storage. The
optional download test contacts Cloudflare only while the user runs that test.

Thor Stream Butler is an independent launcher. It is not affiliated with or
endorsed by NVIDIA, Microsoft, Sony, Valve, Moonlight, Sunshine, AYN, Retroid,
or the providers of installed third-party apps.

### Data safety draft

Answers must be rechecked against the exact signed artifact in the store console.

- **Data collected by the developer:** none.
- **Data shared by the developer:** none.
- **Accounts:** none; account deletion is not applicable.
- **Advertising or tracking:** none.
- **Data processed locally:** installed-app metadata, local host details,
  optional MAC addresses, network measurements, preferences, and optional
  diagnostic event timestamps.
- **User-controlled export:** a user-selected JSON document may contain host
  addresses, MAC addresses, SSIDs, and measurement history. The user chooses
  its storage location and whether history is included.
- **Network transmission:** diagnostics use DNS, ICMP or platform reachability,
  explicit TCP/UDP host access, and an optional HTTPS download test. Cloudflare
  can technically observe the connection's public IP during that optional test;
  Thor Stream Butler does not receive or retain it.
- **Encryption in transit:** the optional download test uses HTTPS. Local-host
  checks use the protocol required for reachability or Wake-on-LAN and do not
  transmit credentials.
- **Deletion:** clearing history/logs or uninstalling the app removes private
  app data. User-created export files remain under user control.

### Required store assets

- 512 × 512 high-resolution icon derived from the original Thor artwork;
- 1024 × 500 feature graphic without protected third-party logos;
- sanitized portrait and landscape phone/handheld screenshots;
- at least one controller-focus screenshot and one completed network assessment;
- English and German captions with identical feature claims;
- no IP addresses, SSIDs, MAC addresses, hostnames, credentials, or personal notifications.

### Publication gate

- [ ] Permanent production signing key has two separate offline backups.
- [ ] APK and AAB are production-signed and both signatures are verified.
- [ ] SHA-256 checksums match the uploaded artifacts.
- [ ] The physical-device plan in `PHYSICAL_TEST_PLAN.md` has no open blocker.
- [ ] Store screenshots and text match the exact release build.
- [ ] Privacy policy URL is public and the Data safety form matches this draft.
- [ ] Closed testing confirms install, update, controller, network, and host flows.
- [ ] Content-rating and target-audience questionnaires were answered for this utility.
- [ ] English and German release notes describe the same facts and limitations.

---

## Deutsch

### Produktmetadaten

- **App-Name:** Thor Stream Butler
- **Kategorie:** Tools
- **Standardsprache:** Englisch (USA)
- **Paketname:** `de.thorstream.butler`
- **Website:** <https://strugglechen1337.github.io/thor-stream-butler/de/>
- **Datenschutzerklärung:** <https://strugglechen1337.github.io/thor-stream-butler/de/datenschutz/>
- **Support:** <https://github.com/Strugglechen1337/thor-stream-butler/issues>

### Kurzbeschreibung

Starte Game-Streams mit Controller-Netzcheck und lokalen Hosts.

### Vollständige Beschreibung

Thor Stream Butler ist ein Local-first-Gaming-Streaming-Launcher für
Android-Handhelds. Er vereint installierte Streaming-Apps, lokale Hosts,
Netzwerkchecks und Wake-on-LAN in einer controllerfreundlichen Oberfläche.

Große fokussierbare Kacheln lassen sich per D-Pad, Controller oder Touch
bedienen. Vor dem Öffnen einer Streaming-App kann Thor Stream Butler Latenz,
Jitter, Paketverlust, DNS, WLAN-Qualität, gemessene Downloadrate und einen
zugewiesenen lokalen Host bewerten. Grüne, gelbe, rote und graue Ergebnisse
erklären verständlich, was gemessen wurde und was verbessert werden kann.

Wichtige Funktionen:

- startbare Android-Apps hinzufügen, ohne Markenassets Dritter einzubauen;
- Kacheln mit Sunshine-, Moonlight-, Xbox-, PlayStation-, Steam-Link- oder eigenen Hosts verknüpfen;
- TCP-Erreichbarkeit prüfen und Wake-on-LAN-Magic-Packets senden;
- lokale Streaming-Profile speichern und konservative Empfehlungen erhalten;
- bis zu 100 letzte Messungen und vergleichbare Verbindungstrends ansehen;
- Konfiguration über die Android-Dateiauswahl exportieren oder importieren;
- Englisch oder Deutsch in Portrait und Landscape verwenden.

Es gibt kein Konto, keine Werbung, kein Tracking, keine Analytics, keine
Telemetrie und kein Cloud-Backend. Netzwerk- und Hostdaten bleiben im privaten
lokalen App-Speicher. Nur beim optionalen Downloadtest wird Cloudflare für die
Dauer des vom Benutzer gestarteten Tests kontaktiert.

Thor Stream Butler ist ein unabhängiger Launcher. Er ist weder mit NVIDIA,
Microsoft, Sony, Valve, Moonlight, Sunshine, AYN oder Retroid noch mit den
Anbietern installierter Drittanbieter-Apps verbunden oder von ihnen empfohlen.

### Data-Safety-Entwurf

Die Angaben müssen im Store für das exakte signierte Artefakt erneut geprüft werden.

- **Vom Entwickler erhobene Daten:** keine.
- **Vom Entwickler geteilte Daten:** keine.
- **Konten:** keine; eine Kontolöschung ist nicht anwendbar.
- **Werbung oder Tracking:** keines.
- **Lokal verarbeitete Daten:** Metadaten installierter Apps, lokale Hosts,
  optionale MAC-Adressen, Netzwerkmessungen, Einstellungen und optionale
  Zeitpunkte von Diagnoseereignissen.
- **Benutzergesteuerter Export:** Ein vom Benutzer gewähltes JSON-Dokument kann
  Host-Adressen, MAC-Adressen, SSIDs und Messhistorie enthalten. Der Benutzer
  bestimmt Speicherort und Einbeziehung der Historie.
- **Netzwerkübertragung:** Diagnosen verwenden DNS, ICMP oder
  Plattform-Erreichbarkeit, ausdrückliche TCP-/UDP-Hostzugriffe sowie einen
  optionalen HTTPS-Downloadtest. Cloudflare kann dabei technisch die öffentliche
  IP der Verbindung sehen; Thor Stream Butler erhält oder speichert sie nicht.
- **Transportverschlüsselung:** Der optionale Downloadtest nutzt HTTPS.
  Lokale Hosttests verwenden das für Erreichbarkeit oder Wake-on-LAN nötige
  Protokoll und übertragen keine Zugangsdaten.
- **Löschung:** Das Leeren von Historie/Log oder die Deinstallation entfernt
  private App-Daten. Vom Benutzer erzeugte Exportdateien bleiben unter seiner Kontrolle.

### Benötigte Store-Assets

- hochauflösendes 512-×-512-Icon auf Basis des eigenen Thor-Artworks;
- Feature-Grafik mit 1024 × 500 Pixeln ohne geschützte Drittanbieterlogos;
- bereinigte Portrait- und Landscape-Screenshots von Smartphone/Handheld;
- mindestens ein Screenshot mit Controller-Fokus und einer mit fertiger Netzwerkbewertung;
- englische und deutsche Bildtexte mit identischen Funktionsaussagen;
- keine IP-Adressen, SSIDs, MAC-Adressen, Hostnamen, Zugangsdaten oder privaten Benachrichtigungen.

### Veröffentlichungs-Gate

- [ ] Der dauerhafte Produktionsschlüssel besitzt zwei getrennte Offline-Sicherungen.
- [ ] APK und AAB sind produktiv signiert und beide Signaturen sind geprüft.
- [ ] SHA-256-Prüfsummen stimmen mit den hochgeladenen Artefakten überein.
- [ ] Der Hardwareplan in `PHYSICAL_TEST_PLAN.md` enthält keinen offenen Blocker.
- [ ] Store-Screenshots und Texte entsprechen dem exakten Release-Build.
- [ ] Die Datenschutz-URL ist öffentlich und das Data-Safety-Formular entspricht diesem Entwurf.
- [ ] Ein geschlossener Test bestätigt Installation, Update, Controller-, Netzwerk- und Hostabläufe.
- [ ] Fragebögen zu Altersfreigabe und Zielgruppe wurden für dieses Dienstprogramm beantwortet.
- [ ] Englische und deutsche Release Notes enthalten dieselben Fakten und Einschränkungen.
