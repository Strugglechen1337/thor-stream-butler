# Physical release validation / Physische Release-Prüfung

The signed production release is blocked until this plan is executed on real
hardware. Test records and screenshots must not contain private network data.

Das signierte Produktionsrelease bleibt blockiert, bis dieser Plan auf echter
Hardware ausgeführt wurde. Testprotokolle und Screenshots dürfen keine privaten
Netzwerkdaten enthalten.

## English

### Minimum device matrix

| Area | Required coverage |
|---|---|
| Primary handheld | AYN Thor or the exact launch device |
| Secondary handheld | One Odin or Retroid Pocket-class device |
| Android versions | Oldest supported Android 9/API 28 and current target/API 37 where available |
| Input | Built-in controls, one external Bluetooth controller, and touch |
| Display | Portrait, landscape, sleep/resume, and one external display if supported |
| Network | 2.4 GHz Wi-Fi, 5/6 GHz Wi-Fi, Ethernet adapter, offline, and VPN |
| Host | Real Sunshine/Moonlight-compatible PC with TCP test and Wake-on-LAN |

Record device model, Android build, app version, signing certificate digest,
controller, and pass/fail only. Replace SSIDs, addresses, MACs, and hostnames
with neutral labels before attaching a record publicly.

### Release-blocking scenarios

1. Install the production-signed APK on a clean device and verify a cold start without a crash or ANR.
2. Update from the previous production-signed version and verify apps, hosts, settings, and history remain intact.
3. Navigate every screen using only D-pad/controller; focus must always be visible and recover after dialogs/back navigation.
4. Repeat core navigation with touch in portrait and landscape; no control may be clipped or unreachable.
5. Add a real installed app, relaunch Thor Stream Butler, edit/reorder the tile, and start the target app.
6. Verify missing or removed target apps show a localized error and never crash.
7. Deny Wi-Fi and local-network permissions, retry each feature, then grant them; unavailable values and recovery must be clear.
8. Run green, yellow, red, gray, offline, DNS-failure, and partial-failure diagnostics; only a complete green result may auto-launch.
9. Test 2.4 GHz and 5/6 GHz Wi-Fi plus Ethernet; confirm transport, link data, latency, jitter, loss, and recommendations are plausible.
10. Add IPv4, bracketed IPv6, scoped IPv6, and hostname hosts; invalid values and ports must remain blocked.
11. Test real host reachability, closed/open TCP ports, Android NSD discovery, and an unreachable host.
12. Send Wake-on-LAN using global broadcast and a configured subnet/unicast target; verify both success and failure messages.
13. Export without and with history, inspect the document privately, import it, and verify atomic rollback for a deliberately invalid copy.
14. Create more than 100 measurements and verify only the latest 100 remain and filtering/trends stay responsive.
15. Switch system language between English and German and verify every release-facing screen, permission explanation, and error path.
16. Run for two hours with repeated diagnostics and 30 app launches; verify no ANR, runaway battery drain, unbounded storage, or leaked dialog.
17. Reboot and repeat app launch, network test, host test, and Wake-on-LAN.

### Acceptance criteria

- no open crash, ANR, data-loss, privacy, signing, launch, controller, or permission blocker;
- no severe accessibility issue for focus visibility, text scaling, contrast, or touch target size;
- cold release start is under three seconds on the primary launch device;
- all stored/exported values match actual measurements or are explicitly unavailable;
- APK and AAB-derived install set have the same version, behavior, and production certificate;
- English and German store/release claims match the observed build.

### Evidence to retain privately

- signed artifact hashes and certificate digest;
- completed matrix with tester/date/build;
- sanitized screenshots or short controller-navigation recording;
- battery, ANR, crash, and storage observations;
- exact list of open non-blocking issues accepted for release.

---

## Deutsch

### Minimale Gerätematrix

| Bereich | Erforderliche Abdeckung |
|---|---|
| Primäres Handheld | AYN Thor oder das exakte Zielgerät des Releases |
| Zweites Handheld | Ein Gerät der Odin- oder Retroid-Pocket-Klasse |
| Android-Versionen | Ältestes unterstütztes Android 9/API 28 und aktuelles Ziel/API 37, soweit verfügbar |
| Eingabe | Eingebaute Steuerung, ein externer Bluetooth-Controller und Touch |
| Anzeige | Portrait, Landscape, Standby/Fortsetzen und falls unterstützt ein externer Bildschirm |
| Netzwerk | 2,4-GHz-WLAN, 5/6-GHz-WLAN, Ethernet-Adapter, Offline und VPN |
| Host | Echter Sunshine-/Moonlight-kompatibler PC mit TCP-Test und Wake-on-LAN |

Nur Gerätemodell, Android-Build, App-Version, Digest des Signaturzertifikats,
Controller und Bestanden/Fehlgeschlagen festhalten. SSIDs, Adressen, MACs und
Hostnamen vor einer öffentlichen Anlage durch neutrale Bezeichnungen ersetzen.

### Release-blockierende Szenarien

1. Produktiv signierte APK auf einem sauberen Gerät installieren und Kaltstart ohne Absturz oder ANR prüfen.
2. Von der vorherigen produktiv signierten Version aktualisieren; Apps, Hosts, Einstellungen und Historie müssen erhalten bleiben.
3. Jeden Bildschirm nur per D-Pad/Controller bedienen; Fokus muss sichtbar bleiben und nach Dialog/Zurück wiederhergestellt werden.
4. Kernnavigation per Touch in Portrait und Landscape wiederholen; kein Element darf abgeschnitten oder unerreichbar sein.
5. Eine echte installierte App hinzufügen, Thor Stream Butler neu starten, Kachel bearbeiten/sortieren und Ziel-App starten.
6. Fehlende oder entfernte Ziel-Apps müssen eine lokalisierte Fehlermeldung ohne Absturz zeigen.
7. WLAN- und lokales Netzwerkrecht verweigern, Funktionen erneut versuchen und Rechte danach gewähren; fehlende Werte und Erholung müssen klar sein.
8. Grüne, gelbe, rote, graue, Offline-, DNS-Fehler- und Teilfehler-Diagnosen ausführen; nur vollständiges Grün darf automatisch starten.
9. 2,4-GHz- und 5/6-GHz-WLAN sowie Ethernet prüfen; Transport, Linkdaten, Latenz, Jitter, Verlust und Empfehlungen müssen plausibel sein.
10. IPv4-, geklammerte IPv6-, Bereichs-IPv6- und Hostname-Hosts anlegen; ungültige Werte und Ports müssen blockiert bleiben.
11. Echte Host-Erreichbarkeit, geschlossene/offene TCP-Ports, Android-NSD-Suche und nicht erreichbaren Host prüfen.
12. Wake-on-LAN mit globalem Broadcast und konfiguriertem Subnetz-/Unicast-Ziel senden; Erfolgs- und Fehlermeldung prüfen.
13. Ohne und mit Historie exportieren, Dokument privat prüfen, importieren und atomaren Rollback mit absichtlich ungültiger Kopie testen.
14. Mehr als 100 Messungen erzeugen; nur die letzten 100 dürfen bleiben und Filter/Trends müssen flüssig sein.
15. Systemsprache zwischen Englisch und Deutsch wechseln und alle releasebezogenen Ansichten, Berechtigungstexte und Fehlerpfade prüfen.
16. Zwei Stunden mit wiederholten Diagnosen und 30 App-Starts laufen lassen; keine ANR, auffällige Akkulast, unbegrenzter Speicher oder hängender Dialog.
17. Gerät neu starten und App-Start, Netzwerktest, Hosttest und Wake-on-LAN wiederholen.

### Abnahmekriterien

- kein offener Blocker bei Absturz, ANR, Datenverlust, Datenschutz, Signatur, App-Start, Controller oder Berechtigung;
- kein schwerer Barrierefreiheitsfehler bei Fokus, Textskalierung, Kontrast oder Touch-Zielgröße;
- Kaltstart des Release-Builds unter drei Sekunden auf dem primären Zielgerät;
- gespeicherte/exportierte Werte entsprechen echten Messungen oder sind ausdrücklich nicht verfügbar;
- APK und AAB-Installationssatz besitzen Version, Verhalten und Produktionszertifikat gemeinsam;
- englische und deutsche Store-/Release-Aussagen entsprechen dem beobachteten Build.

### Privat aufzubewahrende Nachweise

- Hashes der signierten Artefakte und Digest des Zertifikats;
- ausgefüllte Matrix mit Tester, Datum und Build;
- bereinigte Screenshots oder kurze Aufnahme der Controller-Navigation;
- Beobachtungen zu Akku, ANR, Absturz und Speicher;
- exakte Liste offener, für das Release akzeptierter nicht blockierender Probleme.
