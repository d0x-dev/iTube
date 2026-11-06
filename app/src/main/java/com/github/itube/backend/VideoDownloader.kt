package com.github.itube

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

sealed class DownloadOption {
    abstract val label: String
    abstract val formatLabel: String
    abstract val approxSize: String?

    data class Video(
        val height: Int,
        val fps: Int,
        override val label: String,
        override val formatLabel: String,
        override val approxSize: String?,
        val videoStreamUrl: String,
        val audioStreamUrl: String?,
        val isMuxed: Boolean,
    ) : DownloadOption()

    data class Audio(
        val bitrate: Int,
        override val label: String,
        override val formatLabel: String,
        override val approxSize: String?,
        val streamUrl: String,
    ) : DownloadOption()
}

object VideoDownloader {
    private const val TAG = "VideoDownloader"
    private const val CHANNEL_ID = "downloads_channel"
    private const val CHANNEL_NAME = "Downloads"
    private val nextId = AtomicInteger(1000)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress and completion"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun extractOptions(info: StreamInfo): List<DownloadOption> {
        val durationSec = info.duration.takeIf { it > 0 } ?: 0L
        val options = mutableListOf<DownloadOption>()

        // 1. Video Options
        val muxedStreams = info.videoStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.height > 0 }
            .groupBy { it.height }

        val videoOnlyStreams = info.videoOnlyStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.height > 0 }
            .groupBy { it.height }

        val bestAudio = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .sortedWith(
                compareBy<org.schabi.newpipe.extractor.stream.AudioStream> {
                    if (it.format == org.schabi.newpipe.extractor.MediaFormat.M4A) 0 else 1
                }.thenByDescending { it.averageBitrate }
            ).firstOrNull()

        val allHeights = (muxedStreams.keys + videoOnlyStreams.keys).sortedDescending()

        for (height in allHeights) {
            val muxed = muxedStreams[height]?.maxByOrNull { it.fps }
            if (muxed != null) {
                val sizeStr = if (durationSec > 0 && muxed.bitrate > 0) {
                    formatBytes((muxed.bitrate.toLong() * durationSec) / 8)
                } else null

                val fpsStr = if (muxed.fps > 30) " · ${muxed.fps}fps" else ""
                options.add(
                    DownloadOption.Video(
                        height = height,
                        fps = muxed.fps,
                        label = "${height}p",
                        formatLabel = "MP4$fpsStr",
                        approxSize = sizeStr,
                        videoStreamUrl = muxed.content,
                        audioStreamUrl = null,
                        isMuxed = true,
                    )
                )
            } else {
                val videoOnly = videoOnlyStreams[height]?.let { list ->
                    list.filter { it.codec?.startsWith("avc") == true }.maxByOrNull { it.fps * 1000 + it.bitrate }
                        ?: list.maxByOrNull { it.fps * 1000 + it.bitrate }
                }

                if (videoOnly != null) {
                    val audioBitrate = bestAudio?.averageBitrate ?: 128
                    val totalBitrate = videoOnly.bitrate + (audioBitrate * 1000)
                    val sizeStr = if (durationSec > 0 && totalBitrate > 0) {
                        formatBytes((totalBitrate.toLong() * durationSec) / 8)
                    } else null

                    val fpsStr = if (videoOnly.fps > 30) " · ${videoOnly.fps}fps" else ""
                    options.add(
                        DownloadOption.Video(
                            height = height,
                            fps = videoOnly.fps,
                            label = "${height}p",
                            formatLabel = "MP4$fpsStr (HD)",
                            approxSize = sizeStr,
                            videoStreamUrl = videoOnly.content,
                            audioStreamUrl = bestAudio?.content,
                            isMuxed = false,
                        )
                    )
                }
            }
        }

        // 2. Audio Options
        val audios = info.audioStreams
            .filter { it.isUrl && it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .sortedByDescending { it.averageBitrate }

        val m4aAudios = audios.filter { it.format == org.schabi.newpipe.extractor.MediaFormat.M4A }
        val finalAudios = if (m4aAudios.isNotEmpty()) m4aAudios else audios

        finalAudios.forEach { audio ->
            val kbps = audio.averageBitrate
            val sizeStr = if (durationSec > 0 && kbps > 0) {
                formatBytes((kbps.toLong() * 1000L * durationSec) / 8)
            } else null

            val titleLabel = when {
                kbps >= 160 -> "High Quality Audio"
                kbps >= 120 -> "Standard Audio"
                else -> "Audio (${kbps} kbps)"
            }

            options.add(
                DownloadOption.Audio(
                    bitrate = kbps,
                    label = titleLabel,
                    formatLabel = "M4A · ${kbps} kbps",
                    approxSize = sizeStr,
                    streamUrl = audio.content,
                )
            )
        }

        return options
    }

    fun download(context: Context, videoTitle: String, option: DownloadOption) {
        val appContext = context.applicationContext
        init(appContext)
        val notificationId = nextId.incrementAndGet()

        scope.launch {
            runCatching {
                performDownload(appContext, notificationId, videoTitle, option)
            }.onFailure { err ->
                Log.e(TAG, "Download failed for '$videoTitle'", err)
                showFailedNotification(appContext, notificationId, videoTitle, err.message ?: "Download error")
            }
        }
    }

    private suspend fun performDownload(
        context: Context,
        notificationId: Int,
        title: String,
        option: DownloadOption
    ) = withContext(Dispatchers.IO) {
        val nm = NotificationManagerCompat.from(context)
        val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading: $title")
            .setContentText("${option.label} · Starting…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}

        var lastUpdate = 0L
        fun updateProgress(currentProgress: Int, text: String) {
            val now = System.currentTimeMillis()
            if (now - lastUpdate >= 300 || currentProgress == 100) {
                lastUpdate = now
                builder.setProgress(100, currentProgress.coerceIn(0, 100), false)
                builder.setContentText(text)
                try {
                    nm.notify(notificationId, builder.build())
                } catch (_: SecurityException) {}
            }
        }

        val cacheDir = context.cacheDir
        when (option) {
            is DownloadOption.Audio -> {
                val tempFile = File(cacheDir, "dl_${System.currentTimeMillis()}_${cleanTitle}.m4a")
                try {
                    downloadFile(option.streamUrl, tempFile) { read, total ->
                        val percent = if (total > 0) ((read * 100) / total).toInt() else 0
                        val text = "${option.label} · $percent% (${formatBytes(read)} / ${formatBytes(total)})"
                        updateProgress(percent, text)
                    }
                    val fileName = "$cleanTitle [Audio].m4a"
                    val savedUri = saveToDownloads(context, tempFile, fileName, "audio/mp4")
                    showCompleteNotification(context, notificationId, title, option.label, savedUri, "audio/mp4")
                } finally {
                    tempFile.delete()
                }
            }
            is DownloadOption.Video -> {
                if (option.isMuxed || option.audioStreamUrl == null) {
                    val tempFile = File(cacheDir, "dl_${System.currentTimeMillis()}_${cleanTitle}.mp4")
                    try {
                        downloadFile(option.videoStreamUrl, tempFile) { read, total ->
                            val percent = if (total > 0) ((read * 100) / total).toInt() else 0
                            val text = "${option.label} · $percent% (${formatBytes(read)} / ${formatBytes(total)})"
                            updateProgress(percent, text)
                        }
                        val fileName = "$cleanTitle [${option.label}].mp4"
                        val savedUri = saveToDownloads(context, tempFile, fileName, "video/mp4")
                        showCompleteNotification(context, notificationId, title, option.label, savedUri, "video/mp4")
                    } finally {
                        tempFile.delete()
                    }
                } else {
                    // Separate video and audio: download both, then mux
                    val tempVideo = File(cacheDir, "vid_${System.currentTimeMillis()}_$cleanTitle.mp4")
                    val tempAudio = File(cacheDir, "aud_${System.currentTimeMillis()}_$cleanTitle.m4a")
                    val tempMuxed = File(cacheDir, "mux_${System.currentTimeMillis()}_$cleanTitle.mp4")

                    try {
                        // 1. Download video (0% -> 80%)
                        downloadFile(option.videoStreamUrl, tempVideo) { read, total ->
                            val p = if (total > 0) ((read * 80) / total).toInt() else 0
                            updateProgress(p, "Video ${option.label} · $p% (${formatBytes(read)})")
                        }

                        // 2. Download audio (80% -> 95%)
                        downloadFile(option.audioStreamUrl, tempAudio) { read, total ->
                            val p = 80 + (if (total > 0) ((read * 15) / total).toInt() else 0)
                            updateProgress(p, "Audio · $p% (${formatBytes(read)})")
                        }

                        // 3. Mux (95% -> 100%)
                        updateProgress(96, "Muxing video & audio…")
                        val muxSuccess = runCatching {
                            muxAudioVideo(tempVideo, tempAudio, tempMuxed)
                        }.isSuccess

                        val finalFile = if (muxSuccess && tempMuxed.exists() && tempMuxed.length() > 0) {
                            tempMuxed
                        } else {
                            tempVideo
                        }

                        val fileName = "$cleanTitle [${option.label}].mp4"
                        val savedUri = saveToDownloads(context, finalFile, fileName, "video/mp4")
                        showCompleteNotification(context, notificationId, title, option.label, savedUri, "video/mp4")
                    } finally {
                        tempVideo.delete()
                        tempAudio.delete()
                        tempMuxed.delete()
                    }
                }
            }
        }
    }

    private fun downloadFile(url: String, targetFile: File, onProgress: (bytesRead: Long, totalBytes: Long) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Net.USER_AGENT)
            .build()

        Net.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Network error ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            val totalBytes = body.contentLength()
            targetFile.parentFile?.mkdirs()

            targetFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)
                    }
                    output.flush()
                }
            }
        }
    }

    private fun muxAudioVideo(videoFile: File, audioFile: File, outputFile: File) {
        val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
        val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            var videoTrackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            var audioTrackIndex = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(format)
                    break
                }
            }

            if (videoTrackIndex == -1) throw IOException("No video track found")
            muxer.start()

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy video
            while (true) {
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy audio if available
            if (audioTrackIndex != -1) {
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }
        } finally {
            runCatching { muxer.stop() }
            runCatching { muxer.release() }
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
        }
    }

    private fun saveToDownloads(context: Context, sourceFile: File, fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/iTube")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "iTube")
            dir.mkdirs()
            val destFile = File(dir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)
            Uri.fromFile(destFile)
        }
    }

    private fun showCompleteNotification(
        context: Context,
        notificationId: Int,
        title: String,
        quality: String,
        fileUri: Uri?,
        mimeType: String
    ) {
        val nm = NotificationManagerCompat.from(context)

        val pendingIntent = fileUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText("$title · $quality")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun showFailedNotification(
        context: Context,
        notificationId: Int,
        title: String,
        reason: String
    ) {
        val nm = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("$title · $reason")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
