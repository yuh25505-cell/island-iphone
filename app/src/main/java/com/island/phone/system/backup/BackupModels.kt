package com.island.phone.system.backup

enum class ImportMode { OVERWRITE, MERGE }

enum class RejectReason {
    NOT_A_BACKUP,
    BROKEN_DATA,
    BACKUP_TOO_NEW,
    VERSION_MISMATCH_FOR_MERGE,
    READ_FAILED,
}

sealed interface ImportResult {
    data object NeedRestart : ImportResult
    data object Merged : ImportResult
    data class Rejected(val reason: RejectReason) : ImportResult
}
