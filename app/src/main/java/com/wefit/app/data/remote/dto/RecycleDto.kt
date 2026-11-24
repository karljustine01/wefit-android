package com.wefit.app.data.remote.dto

data class RecycleItemDto(
    val type: String,          // section | exercise | assignment
    val id: Int,
    val title: String,
    val subtitle: String,
    val deleted_at: String,
    val purge_at: String,
    val days_left: Int
)

data class RecycleBinDto(
    val retention_days: Int,
    val items: List<RecycleItemDto>
)

data class RecyclePolicyDto(
    val bin_days: Int,
    val videos_days: Int,
    val notifications_read_days: Int,
    val notifications_any_days: Int,
    val activity_logs_days: Int,
    val progress_points_days: Int,
    val cancelled_session_days: Int,
    val deleted_account_days: Int = 30
)

data class RecycleRunDto(
    val id: Int,
    val trigger: String,
    val bytes_freed: Long,
    val total_rows: Int,
    val created_at: String
)

data class RecycleStatsDto(
    val policy: RecyclePolicyDto,
    val bin_items: Int,
    val videos: Int,
    val video_bytes: Long,
    val notifications: Int,
    val activity_logs: Int,
    val progress_points: Int,
    val pending_cleanup: Int,
    val last_runs: List<RecycleRunDto>
)

data class RecycleRunResultDto(
    val dry_run: Boolean,
    val total_rows: Int,
    val bytes_freed: Long
)
