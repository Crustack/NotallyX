---
sessionId: session-260824-172958-1864
---

# Findings

### Task

Analysis only — **no code was added or changed**. Goal: identify the most likely causes of users' `NotallyDatabase` being completely wiped while attachments in the media folders survived, and of the reported `SQLiteDatabaseCorruptException: database disk image is malformed`.

### The wipe mechanism (very high confidence)

The deletion is performed by Room/Android, not by app code.

`NotallyDatabase.kt:161-232` builds the database without any `DatabaseErrorHandler` and without overriding `onCorruption`:

```kotlin
Room.databaseBuilder(context, NotallyDatabase::class.java, getCurrentDatabaseName(...))
    .addMigrations(Migration2, ... Migration11)
    // plaintext case → default FrameworkSQLiteOpenHelperFactory
```

Verified chain against the artifacts this project actually resolves (`room-runtime:2.6.1`, `sqlite-framework:2.4.0`, `sqlite:2.4.0`):

1. `android.database.sqlite.SQLiteQuery.fillWindow()` catches `SQLiteDatabaseCorruptException` and calls `onCorruption()`.
2. `FrameworkSQLiteOpenHelper` (line ~128) forwards it to `callback.onCorruption(db)`.
3. Room's `RoomOpenHelper` does **not** override `onCorruption` (no occurrence in room-runtime 2.6.1 sources).
4. The inherited `SupportSQLiteOpenHelper.Callback.onCorruption()` logs `"Corruption reported by sqlite on database: …"`, then `"deleting the database file: …"`, then calls `SQLiteDatabase.deleteDatabase(File)` — which removes `NotallyDatabase`, `-wal`, `-shm`, `-journal` and `*-mj*`.

The reported stack trace is exactly this path (`SQLiteCursor.getCount → fillWindow → SQLiteQuery.fillWindow`), triggered by `getAllPinnedToStatusNotes`, which runs at every cold start from `NotallyXApplication.restorePinnedNotifications()` (`NotallyXApplication.kt:165-177`) in an un-guarded `Dispatchers.IO` coroutine. The DB is deleted *before* the exception surfaces, and the exception may be swallowed — matching "there is not always an exception directly before".

**Asymmetry worth checking against the reports:** only the plaintext path deletes. With biometric lock the SQLCipher helper is used and `net.zetetic.database.DefaultDatabaseErrorHandler.onCorruption()` returns early (`if (SQLiteDatabase.hasCodec()) return;`) without deleting.

### Ranked culprits

 # | Culprit | Confidence |
---|---|---|
 1 | Room's default `onCorruption` **deletes** the DB + `-wal`/`-shm`; any transient corruption becomes total, silent loss | Very high |
 2 | SharedPreferences excluded from backup while the DB domain is included ⇒ `dataInPublicFolder` resets to `false` ⇒ app opens/creates an **empty internal DB** while the real one sits in `Android/media/...` ("wipe" with no exception) | High |
 3 | Non-atomic `copyTo(overwrite = true)` over the live DB file + stale `-wal`/`-shm` never deleted (biometric-lock enable/disable, public-folder switch) | High |
 4 | Broken DB singleton / leaked never-closed instances, plus platform SQLite and bundled SQLCipher SQLite concurrently on the same WAL file | High |
 5 | Android Auto Backup of a live WAL database ⇒ inconsistent restore ⇒ corruption ⇒ #1 | Medium-high |
 6 | `ErrorActivity` (`:error_activity` process) calling `deleteDatabase()` before a reimport that can fail | Medium |
 7 | DB stored in `Android/media/...` (FUSE/SD-card locking, deletion by cleaner apps/MediaStore) | Medium |

### Evidence per culprit

**#2 — wrong file, not a wipe.** `res/xml/backup_content.xml` and `res/xml/data_rules.xml` exclude only `domain="sharedpref"`, so the `database` domain is backed up and restored while prefs are not. `dataInPublicFolder` (`NotallyXPreferences.kt:206`, default `false`), `dataSchemaId` (`:238`), `iv`, `databaseEncryptionKey` and `biometricLock` are all preferences. After a cloud restore / device transfer / prefs loss:
- public-folder users get `dataInPublicFolder = false` ⇒ `getCurrentDatabaseName()` returns the internal name ⇒ Room creates a fresh empty DB; the real data is still on disk and recoverable;
- `dataSchemaId` resets ⇒ `runMigrations()` re-runs attachment moves and note splitting;
- biometric-lock users get an encrypted DB with no Keystore key and `preferences.iv.value!!` (`NotallyDatabase.kt:239`) ⇒ NPE / permanently unopenable DB.

**#3 — non-atomic overwrite.** `BaseNoteModel.kt:326-375` (`enableBiometricLock`/`disableBiometricLock`) does `dbFileCopy.copyToLarge(originalDbFile, overwrite = true)`; Kotlin's `copyTo(overwrite = true)` deletes the target first and streams without `fsync`, so interruption leaves a truncated/0-byte DB. The old `-wal`/`-shm` are never removed, so the next open recovers a WAL that belongs to a different database. `SQLCipherUtils.java:190-278` has the same defect (`originalFile.delete(); newFile.renameTo(originalFile);`, return value unchecked, `-wal`/`-shm` ignored) and its own javadoc forbids running it while the DB is open. `enableDataInPublic`/`disableDataInPublic` (`BaseNoteModel.kt:262-324`) copy `db`, `-wal`, `-shm` one-by-one with the DB still open (only `checkpoint()`, never `close()`).

**#4 — multiple connections / two SQLite libraries.** `NotallyDatabase.getDatabase()` (`:104-115`) is a double-checked lock with **no re-check inside `synchronized`**, so concurrent callers each build a `RoomDatabase` and the loser is leaked open. The `biometricLock` / `dataInPublicFolder` observers (`:200-228`) `postValue(newInstance)` without closing the old one; `getFreshDatabase()` adds more. Meanwhile `SQLCipherUtils.getDatabaseState()` (called from `:183`/`:192` on every `createInstance`) and `copyDatabase()` (`ExportExtensions.kt:565-586`, invoked on **every note save** when backup-on-save is on) open the *live* file with the bundled `net.zetetic` SQLite — including `OPEN_READWRITE` + `ATTACH` + `sqlcipher_export`. The `-shm` wal-index format is implementation-specific; concurrent WAL access from two SQLite builds is unsupported. A leaked connection closing after the file body was swapped checkpoints stale pages into the new file.

**#6 — second process.** `AndroidManifest.xml:151` puts `ErrorActivity` in `:error_activity`; `ErrorActivity.kt:200-223` runs `copyDatabase()` → `deleteDatabase(DATABASE_NAME)` → `clearInstance()` → `importRawDatabase(uri, …)`. A truncated export, a failing import, or a public-folder user (where `deleteDatabase` only touches the internal path) ends with an empty DB — and this path is offered exactly to users who already crashed.

### Secondary data-loss issues (not full wipes)

- `copyDatabase()` copies only the main DB file and **discards the result of `pragma wal_checkpoint(FULL)`**; a busy checkpoint (likely given #4) means backups can silently lack the newest notes.
- `DataSchemaMigrations.kt:95-99` deletes notes (`dao.delete(id)`) that cannot be read/repaired.
- `Migration3/4/5/7` use backticks for string defaults and `Migration6` uses `DEFAULT 'timestamp'` on an `INTEGER NOT NULL` column (`NotallyDatabase.kt:259-292`).
- `SQLCipherUtils.getDatabaseState()` returns `ENCRYPTED` for *any* open failure (`SQLCipherUtils.java:76-77`), so a merely damaged plaintext DB is misclassified and the app flips `biometricLock` to `ENABLED`.
- `SELECT * FROM BaseNote` with bodies up to `MAX_BODY_SIZE_MB = 1.5` (`BaseNoteDao.kt:38`, `:119-130`) stresses the 2 MB `CursorWindow` on exactly the crashing query.
- `Runtime.getRuntime().exit(0)` (`UiExtensions.kt:1027`, `SettingsFragment.kt:305`) hard-kills the process and can race with the copies in #3.

### How to confirm from existing reports

- Search user logs for `Corruption reported by sqlite on database:` and `deleting the database file:` — the fingerprint of #1.
- Correlate affected users with: biometric lock on/off (#1 only bites when off), "data in public folder" on/off (#2/#7), recent device restore/new phone/reinstall (#2/#5), recent lock toggle (#3).
- Ask whether `Android/media/com.philkes.notallyx/NotallyDatabase` still exists (⇒ #2, data recoverable) and whether `-wal`/`-shm` survived while the main file vanished (handler deletion vs. truncated copy).

# Proposed Hardening

### Status

Nothing here has been implemented — the task was analysis only. This tab records the fixes implied by the findings so they can be approved (or rejected) as follow-up work.

### P0 — stop turning corruption into data loss

- Install a non-destructive corruption handler for both the plaintext and the SQLCipher path in `NotallyDatabase.createInstance` (custom `SupportSQLiteOpenHelper.Factory` wrapping `FrameworkSQLiteOpenHelperFactory`, or an `openHelperFactory` that supplies a `DatabaseErrorHandler`). On corruption: **rename** `NotallyDatabase*` to `NotallyDatabase-corrupt-<timestamp>`, log it, notify the user, and offer restore from the newest ZIP backup — never delete.
- Guard `NotallyXApplication.restorePinnedNotifications()` so a DB read failure at startup cannot escape unhandled.

### P0 — stop losing users' storage-location preference

- Exclude the `database` domain (and the DB path in the media dir) from `res/xml/backup_content.xml` and `res/xml/data_rules.xml`, or include the SharedPreferences so DB and prefs are always restored as a consistent pair.
- On startup, if the internal DB is missing/empty but a DB exists in the external media folder (or vice-versa), detect it and offer recovery instead of silently creating an empty database.

### P1 — make every DB file replacement atomic

- Single helper used by `enableBiometricLock`, `disableBiometricLock`, `enableDataInPublic`, `disableDataInPublic`: `close()` the Room instance → write to `<name>.tmp` → `fsync` → delete `-wal`/`-shm` of the target → `rename` into place → reopen and `ping()`; roll back from the retained original on any failure.
- Apply the same `-wal`/`-shm` handling inside `SQLCipherUtils.encrypt`/`decrypt` and check the `renameTo` result.

### P1 — one connection, one SQLite implementation

- Fix the double-checked locking in `NotallyDatabase.getDatabase()` and close the previous instance in the `biometricLock`/`dataInPublicFolder` observers and in `getFreshDatabase`.
- Cache the encrypted/plaintext state instead of probing the live file with `SQLCipherUtils.getDatabaseState()` on every `createInstance`; never open the live file with the zetetic library while Room holds it.
- Distinguish "unopenable" from "encrypted" in `getDatabaseState` so a damaged plaintext DB is no longer treated as encrypted.

### P2 — recovery paths and backup fidelity

- Remove `deleteDatabase()` from `ErrorActivity`'s reimport flow; import into a temp DB, verify, then swap atomically.
- Check the return value of `wal_checkpoint(FULL)` in `copyDatabase()` and either retry or include `-wal` in the copy, so backups cannot silently miss recent notes.
- Reconsider `dao.delete(id)` in `splitOversizedNotes()` — quarantine instead of delete.

# Delivery Steps

###   Step 1: Confirm the deletion mechanism against real reports
The team knows, per affected user, whether the DB was deleted by Room's corruption handler, replaced by an empty file, or simply looked for in the wrong location.

- Ask reporters to search their logs for the two fingerprint lines `Corruption reported by sqlite on database:` and `deleting the database file:` (emitted by `SupportSQLiteOpenHelper.Callback.onCorruption`).
- Correlate each report with: biometric lock enabled/disabled (only the plaintext path deletes, because zetetic's handler returns early on `hasCodec()`), "data in public folder" enabled/disabled, and whether the loss followed a device restore, reinstall, or a lock toggle.
- Ask whether `Android/media/com.philkes.notallyx/NotallyDatabase` still exists (indicates the preference-reset scenario, and the data is recoverable) and whether `NotallyDatabase-wal`/`-shm` survived while the main file vanished (handler deletion vs. a truncated `copyTo`).
- Record the outcome so the P0 items can be prioritised by real frequency rather than by theory.

###   Step 2: Install a non-destructive corruption handler
A corrupt database is quarantined and reported instead of being silently deleted by Room.

- Add a corruption handler for the plaintext path in `NotallyDatabase.createInstance` by wrapping `FrameworkSQLiteOpenHelperFactory` (or supplying a `DatabaseErrorHandler`) so `onCorruption` renames `NotallyDatabase`, `-wal`, `-shm` to `NotallyDatabase-corrupt-<timestamp>` instead of calling `SQLiteDatabase.deleteDatabase`.
- Mirror the same behaviour for the SQLCipher path via `SupportOpenHelperFactory`, so both configurations behave identically.
- Surface the event: log it through `ContextWrapper.log`, post a notification, and offer restore from the newest ZIP in the configured backups folder.
- Wrap the startup read in `NotallyXApplication.restorePinnedNotifications()` so a DB failure there cannot escape as an unhandled coroutine exception.

###   Step 3: Stop losing the storage-location preference across backup/restore
A restored or reinstalled app can no longer end up reading an empty internal database while the real one sits in the media folder.

- Update `res/xml/backup_content.xml` and `res/xml/data_rules.xml` so the `database` domain is excluded (or so SharedPreferences are backed up together with the DB), removing today's asymmetry where the DB is restored but `dataInPublicFolder`, `dataSchemaId`, `iv` and `databaseEncryptionKey` are not.
- Add a startup consistency check in `NotallyDatabase`/`NotallyXApplication`: if the internal DB is missing or empty while a DB exists at `getExternalDatabaseFile()` (or the reverse), do not silently create a fresh DB — report the situation and offer to adopt the existing file.
- Handle the restored-but-encrypted case explicitly instead of `preferences.iv.value!!` in `initializeDecryption`: detect the missing Keystore key/IV and show an actionable message.

###   Step 4: Make every replacement of the live database file atomic
Enabling/disabling biometric lock and switching the storage location can no longer truncate the DB or leave a mismatched WAL behind.

- Introduce one `replaceDatabaseFile(...)` helper and use it from `BaseNoteModel.enableBiometricLock`, `disableBiometricLock`, `enableDataInPublic` and `disableDataInPublic`: close the Room instance, write to `<name>.tmp`, `fsync`, delete the target's `-wal`/`-shm`, `rename` into place, reopen and `ping()`, rolling back from the retained original on failure.
- Replace the `copyToLarge(originalDbFile, overwrite = true)` calls (which delete the target before streaming) with that helper.
- Fix `SQLCipherUtils.encrypt`/`decrypt` to delete the companion `-wal`/`-shm` files and to check the `renameTo` result instead of ignoring it.
- Make `enableDataInPublic`/`disableDataInPublic` close the database before copying, rather than relying on `checkpoint()` alone while writers are active.

###   Step 5: Enforce a single connection and a single SQLite implementation
Only one Room instance is alive per database file, and the live file is never touched by a second SQLite library.

- Fix the double-checked locking in `NotallyDatabase.getDatabase()` (re-check `instance` inside `synchronized`) so concurrent callers cannot each build and leak a `RoomDatabase`.
- Close the previous instance in the `biometricLock` and `dataInPublicFolder` observers and in `getFreshDatabase`, instead of only `postValue`-ing the replacement.
- Cache the encrypted/plaintext state rather than probing the live file with `SQLCipherUtils.getDatabaseState()` on every `createInstance`, and stop opening the live file with the zetetic library from `copyDatabase()` while Room holds it open.
- Make `getDatabaseState` distinguish "failed to open" from "encrypted" so a damaged plaintext DB is no longer misclassified and does not flip `biometricLock` to `ENABLED`.

###   Step 6: Harden the crash-recovery and backup paths
The recovery flow offered after a crash can no longer be the thing that finishes off the data, and backups stop silently missing recent notes.

- Rework `ErrorActivity`'s reimport flow: remove the `deleteDatabase(NotallyDatabase.DATABASE_NAME)` call, import into a temporary database, verify it contains the expected tables/row counts, then swap it in atomically via the helper from the earlier stage.
- Avoid opening the database from the `:error_activity` process while the main process may still hold it; operate on a copy only.
- Check the result of `pragma wal_checkpoint(FULL)` in `NotallyDatabase.checkpoint()`/`copyDatabase()` and retry or include `-wal` in the copy, so exported ZIPs cannot omit the newest commits.
- Replace the `dao.delete(id)` fallback in `splitOversizedNotes()` with quarantining the unreadable row, so a migration never destroys a note.