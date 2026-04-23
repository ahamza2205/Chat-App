package com.aa.chatapp.feature.chat.presentation.chat.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isPaused: Boolean = false
        private set

    fun start(): File {
        val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
        val file = File(dir, "vn_${System.currentTimeMillis()}.m4a")
        outputFile = file
        isPaused = false

        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun pause() {
        if (!isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { recorder?.pause(); isPaused = true } catch (_: Exception) {}
        }
    }

    fun resume() {
        if (isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { recorder?.resume(); isPaused = false } catch (_: Exception) {}
        }
    }

    fun getMaxAmplitude(): Int {
        return try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
    }

    fun stop(): Pair<File, Long>? {
        return try {
            if (isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder?.resume()
            }
            recorder?.apply { stop(); release() }
            recorder = null
            isPaused = false
            val file = outputFile ?: return null
            val duration = extractDuration(file)
            file to duration
        } catch (e: Exception) {
            cancel()
            null
        }
    }

    fun cancel() {
        try {
            if (isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try { recorder?.resume() } catch (_: Exception) {}
            }
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        recorder = null
        isPaused = false
        outputFile?.delete()
        outputFile = null
    }

    private fun extractDuration(file: File): Long {
        return try {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(context, Uri.fromFile(file))
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) { 0L }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else MediaRecorder()
    }
}
