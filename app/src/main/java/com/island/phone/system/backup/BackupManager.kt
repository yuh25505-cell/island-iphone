package com.island.phone.system.backup

import android.content.Context
import android.net.Uri
import com.island.phone.data.db.DB_NAME
import com.island.phone.data.db.DB_VERSION
import com.island.phone.data.db.IslandDatabase
import com.island.phone.data.files.autoBackupDir
import com.island.phone.data.files.mediaDir
import com.island.phone.data.files.settingsFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MANIFEST_ENTRY = "manifest.json"
private const val DB_ENTRY = "island.db"
private const val SETTINGS_ENTRY = "settings.preferences_pb"
private const val MEDIA_PREFIX = "media/"
private const val BACKUP_FORMAT = "island-backup"
private const val KEEP_AUTO_BACKUPS = 3

class BackupManager(
    private val context: Context,
    private val database: IslandDatabase,
) {

    suspend fun exportTo(target: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(target)
                ?: error("cannot open target")
            stream.use { writeZip(it) }
        }.isSuccess
    }

    suspend fun autoBackup(): File? = withContext(Dispatchers.IO) {
        val dir = context.autoBackupDir()
        val file = File(dir, "backup-${System.currentTimeMillis()}.zip")
        val ok = runCatching { file.outputStream().use { writeZip(it) } }.isSuccess
        if (!ok) {
            file.delete()
            return@withContext null
        }
        dir.listFiles()
            ?.sortedByDescending { it.name }
            ?.drop(KEEP_AUTO_BACKUPS)
            ?.forEach { it.delete() }
        file
    }

    suspend fun importFrom(source: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "import_tmp")
        temp.deleteRecursively()
        temp.mkdirs()
        try {
            val extracted = runCatching {
                val stream = context.contentResolver.openInputStream(source)
                    ?: error("cannot open source")
                stream.use { extract(it, temp) }
            }
            if (extracted.isFailure) {
                return@withContext ImportResult.Rejected(RejectReason.READ_FAILED)
            }

            val manifestFile = File(temp, MANIFEST_ENTRY)
            val dbFile = File(temp, DB_ENTRY)
            if (!manifestFile.exists() || !dbFile.exists()) {
                return@withContext ImportResult.Rejected(RejectReason.NOT_A_BACKUP)
            }
            val manifest = runCatching { JSONObject(manifestFile.readText()) }.getOrNull()
            if (manifest == null || manifest.optString("format") != BACKUP_FORMAT) {
                return@withContext ImportResult.Rejected(RejectReason.NOT_A_BACKUP)
            }
            val backupDbVersion = manifest.optInt("dbVersion", -1)
            if (backupDbVersion < 1) {
                return@withContext ImportResult.Rejected(RejectReason.NOT_A_BACKUP)
            }
            if (backupDbVersion > DB_VERSION) {
                return@withContext ImportResult.Rejected(RejectReason.BACKUP_TOO_NEW)
            }
            if (!looksLikeSqlite(dbFile)) {
                return@withContext ImportResult.Rejected(RejectReason.BROKEN_DATA)
            }
            if (mode == ImportMode.MERGE && backupDbVersion != DB_VERSION) {
                return@withContext ImportResult.Rejected(RejectReason.VERSION_MISMATCH_FOR_MERGE)
            }

            autoBackup()

            when (mode) {
                ImportMode.OVERWRITE -> overwrite(temp)
                ImportMode.MERGE -> merge(temp)
            }
        } catch (e: Exception) {
            ImportResult.Rejected(RejectReason.BROKEN_DATA)
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun overwrite(temp: File): ImportResult {
        val dbPath = context.getDatabasePath(DB_NAME)
        database.close()
        File(dbPath.path + "-wal").delete()
        File(dbPath.path + "-shm").delete()
        dbPath.delete()
        File(temp, DB_ENTRY).copyTo(dbPath, overwrite = true)

        val settings = File(temp, SETTINGS_ENTRY)
        if (settings.exists()) {
            val target = context.settingsFile()
            target.parentFile?.mkdirs()
            settings.copyTo(target, overwrite = true)
        }

        val media = context.mediaDir()
        media.deleteRecursively()
        media.mkdirs()
        copyTree(File(temp, MEDIA_PREFIX.trimEnd('/')), media, overwrite = true)
        return ImportResult.NeedRestart
    }

    private fun merge(temp: File): ImportResult {
        val db = database.openHelper.writableDatabase
        db.execSQL("ATTACH DATABASE ? AS backup", arrayOf<Any>(File(temp, DB_ENTRY).absolutePath))
        try {
            db.execSQL(
                "INSERT INTO messages (charId, sessionId, senderType, kind, content, filePath, createdAt) " +
                    "SELECT b.charId, b.sessionId, b.senderType, b.kind, b.content, b.filePath, b.createdAt " +
                    "FROM backup.messages b WHERE NOT EXISTS (" +
                    "SELECT 1 FROM main.messages m WHERE m.charId = b.charId " +
                    "AND m.sessionId = b.sessionId AND m.createdAt = b.createdAt " +
                    "AND m.senderType = b.senderType AND m.content = b.content)",
            )
        } finally {
            db.execSQL("DETACH DATABASE backup")
        }
        copyTree(File(temp, MEDIA_PREFIX.trimEnd('/')), context.mediaDir(), overwrite = false)
        return ImportResult.Merged
    }

    private fun writeZip(out: OutputStream) {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(buildManifest().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            addFile(zip, context.getDatabasePath(DB_NAME), DB_ENTRY)
            addFile(zip, context.settingsFile(), SETTINGS_ENTRY)

            val media = context.mediaDir()
            media.walkTopDown().filter { it.isFile }.forEach { file ->
                val relative = file.relativeTo(media).path.replace(File.separatorChar, '/')
                addFile(zip, file, MEDIA_PREFIX + relative)
            }
        }
    }

    private fun buildManifest(): String {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        return JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("formatVersion", 1)
            .put("dbVersion", DB_VERSION)
            .put("appVersion", versionName)
            .put("createdAt", System.currentTimeMillis())
            .toString()
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun extract(input: InputStream, targetDir: File) {
        val root = targetDir.canonicalFile
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val allowed = name == MANIFEST_ENTRY || name == DB_ENTRY ||
                    name == SETTINGS_ENTRY || name.startsWith(MEDIA_PREFIX)
                if (!entry.isDirectory && allowed) {
                    val dest = File(root, name).canonicalFile
                    if (dest.path.startsWith(root.path + File.separator)) {
                        dest.parentFile?.mkdirs()
                        dest.outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun copyTree(from: File, to: File, overwrite: Boolean) {
        if (!from.exists()) return
        from.walkTopDown().filter { it.isFile }.forEach { file ->
            val dest = File(to, file.relativeTo(from).path)
            if (overwrite || !dest.exists()) {
                dest.parentFile?.mkdirs()
                file.copyTo(dest, overwrite = true)
            }
        }
    }

    private fun looksLikeSqlite(file: File): Boolean {
        val header = ByteArray(16)
        val read = file.inputStream().use { it.read(header) }
        return read == 16 && String(header, Charsets.ISO_8859_1) == "SQLite format 3\u0000"
    }
}
