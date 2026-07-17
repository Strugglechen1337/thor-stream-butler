# Release guide / Release-Anleitung

Every Thor Stream Butler release must be complete in English and German. Use
[`.github/RELEASE_TEMPLATE.md`](../.github/RELEASE_TEMPLATE.md) as the release
body and keep both sections equivalent.

Jedes Thor-Stream-Butler-Release muss auf Englisch und Deutsch vollständig sein.
Verwende [`.github/RELEASE_TEMPLATE.md`](../.github/RELEASE_TEMPLATE.md) als
Release-Text und halte beide Abschnitte inhaltlich gleichwertig.

## English

### Before creating a release

1. Choose the version and update every version reference consistently.
2. Move completed entries from `Unreleased` into a dated version in `CHANGELOG.md`.
3. Describe features, fixes, limitations, migration steps, and known issues in English and German.
4. Run unit tests, lint, the debug build, and the instrumented-test build.
5. Build the configured signed release APK. Never publish a debug APK as a production release.
6. Generate and verify a SHA-256 checksum for every downloadable APK.
7. Test installation or update on representative Android hardware.
8. Replace or sanitize screenshots so they contain no private network data.

### Development preview exception

A debug-signed APK may be published only as an explicitly marked GitHub
**prerelease**, never as a production or stable release. Its release notes must
identify the debug signature, explain that a later production-signed build
requires uninstalling the preview and losing its local app data, and disclose
when installation on physical hardware was not verified.

### GitHub release body

- Keep **English first** and **Deutsch second** for a predictable structure.
- Use the same version, facts, links, warnings, and installation steps in both sections.
- Keep product names factual and do not add protected service logos without licensed assets.
- State explicitly when a function is unavailable or not part of the release.
- Donations are optional and must never be presented as required for functionality.

### After publishing

1. Verify the release page, APK, and checksum without an authenticated session.
2. Verify the English and German project pages.
3. Confirm that Android CI and GitHub Pages are green.
4. Add the new release link to the README when downloads become available.

## Deutsch

### Vor dem Erstellen eines Releases

1. Version festlegen und alle Versionsangaben einheitlich aktualisieren.
2. Fertige Einträge aus `Unreleased` in eine datierte Version in `CHANGELOG.md` verschieben.
3. Funktionen, Fehlerbehebungen, Einschränkungen, Migration und bekannte Probleme auf Englisch und Deutsch beschreiben.
4. Unit Tests, Lint, Debug-Build und instrumentierten Test-Build ausführen.
5. Die konfigurierte signierte Release-APK bauen. Niemals eine Debug-APK als produktives Release veröffentlichen.
6. Für jede herunterladbare APK eine SHA-256-Prüfsumme erzeugen und prüfen.
7. Installation oder Update auf repräsentativer Android-Hardware testen.
8. Screenshots ersetzen oder bereinigen, sodass sie keine privaten Netzwerkdaten enthalten.

### Ausnahme für Entwicklungsvorschauen

Eine debug-signierte APK darf ausschließlich als ausdrücklich gekennzeichnetes
GitHub-**Prerelease** veröffentlicht werden, niemals als produktives oder stabiles
Release. Die Release Notes müssen die Debug-Signatur nennen, erklären, dass vor
einem späteren produktiv signierten Build die Vorschau einschließlich ihrer lokalen
App-Daten deinstalliert werden muss, und offenlegen, wenn die Installation auf
echter Hardware nicht geprüft wurde.

### GitHub-Release-Text

- Für eine einheitliche Struktur **English zuerst** und **Deutsch danach** verwenden.
- Version, Fakten, Links, Warnungen und Installationsschritte in beiden Abschnitten gleich halten.
- Produktnamen sachlich verwenden und keine geschützten Logos ohne lizenzierte Assets hinzufügen.
- Deutlich kennzeichnen, wenn eine Funktion nicht verfügbar oder nicht Teil des Releases ist.
- Unterstützung ist freiwillig und darf niemals als Voraussetzung für Funktionen dargestellt werden.

### Nach der Veröffentlichung

1. Release-Seite, APK und Prüfsumme ohne angemeldete Sitzung prüfen.
2. Englische und deutsche Projektseite prüfen.
3. Sicherstellen, dass Android CI und GitHub Pages grün sind.
4. Den neuen Release-Link in der README ergänzen, sobald Downloads verfügbar sind.

## Signing setup / Signing-Einrichtung

### English

Local signed builds read `signing/keystore.properties` (the whole `signing/`
directory is gitignored — back up the keystore externally, otherwise future
app updates become impossible):

```properties
storeFile=thor-stream-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Place the keystore at `signing/thor-stream-release.jks` and run
`./gradlew assembleRelease`. Without these values the release build stays
unsigned; debug builds are unaffected.

The `Release` workflow (`.github/workflows/release.yml`) builds and publishes a
signed APK plus SHA-256 checksum for every `v*` tag. It requires the repository
secrets `SIGNING_KEYSTORE_BASE64` (base64 of the keystore file),
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, and `SIGNING_KEY_PASSWORD`, and
bilingual release notes at `docs/release-notes/<tag>.md`.

### Deutsch

Lokale signierte Builds lesen `signing/keystore.properties` (das gesamte
`signing/`-Verzeichnis ist gitignored — Keystore unbedingt extern sichern,
sonst sind keine App-Updates mehr möglich):

```properties
storeFile=thor-stream-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Den Keystore unter `signing/thor-stream-release.jks` ablegen und
`./gradlew assembleRelease` ausführen. Ohne diese Werte bleibt der
Release-Build unsigniert; Debug-Builds sind nicht betroffen.

Der `Release`-Workflow (`.github/workflows/release.yml`) baut und
veröffentlicht für jeden `v*`-Tag eine signierte APK samt SHA-256-Prüfsumme.
Er benötigt die Repository-Secrets `SIGNING_KEYSTORE_BASE64` (Base64 der
Keystore-Datei), `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` und
`SIGNING_KEY_PASSWORD` sowie zweisprachige Release Notes unter
`docs/release-notes/<tag>.md`.
