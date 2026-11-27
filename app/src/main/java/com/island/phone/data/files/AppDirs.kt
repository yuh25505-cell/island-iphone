package com.island.phone.data.files

import android.content.Context
import java.io.File

fun Context.mediaDir(): File = File(filesDir, "media").apply { mkdirs() }

fun Context.autoBackupDir(): File = File(filesDir, "auto_backups").apply { mkdirs() }

fun Context.settingsFile(): File = File(filesDir, "datastore/settings.preferences_pb")
