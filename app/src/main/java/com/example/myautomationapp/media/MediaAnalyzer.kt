package com.example.myautomationapp.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

class MediaAnalyzer(private val context: Context) {
    data class MediaInfo(
        val displayName: String,
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val rotation: Int,
        val mimeType: String?
    )

    fun inspect(uri: Uri): Result<MediaInfo> = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            MediaInfo(
                displayName = uri.lastPathSegment ?: "Selected media",
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0,
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0,
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            )
        } finally { retriever.release() }
    }
}
