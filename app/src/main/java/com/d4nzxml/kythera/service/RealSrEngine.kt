package com.d4nzxml.kythera.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object RealSrEngine {

    private const val TAG = "Kythera_AI"
    private var isReady = false

    private val BINARIES = listOf(
        "realsr/realsr-ncnn",
        "realsr/libncnn.so",
        "realsr/libc++_shared.so",
        "realsr/libomp.so"
    )

    // 🔥 Kunci ke folder Kythera v3 lu
    private const val MODEL_FOLDER = "realsr/models-Real-ESRGANv3-anime"

    // Copy semua ukuran x2, x3, dan x4
    private val MODEL_FILES = listOf(
        "x2.bin", "x2.param",
        "x3.bin", "x3.param",
        "x4.bin", "x4.param"
    )

    suspend fun setup(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isReady) return@withContext true

        try {
            val baseDir = context.filesDir

            // 1. Copy binary
            for (assetPath in BINARIES) {
                val outFile = File(baseDir, assetPath)
                outFile.parentFile?.mkdirs()

                try {
                    if (!outFile.exists()) {
                        context.assets.open(assetPath).use { input ->
                            FileOutputStream(outFile).use { output -> input.copyTo(output) }
                        }
                    }
                    if (assetPath.endsWith("realsr-ncnn")) {
                        outFile.setExecutable(true, false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Binary absen: $assetPath")
                }
            }

            // 2. Copy semua ukuran model
            for (file in MODEL_FILES) {
                val assetPath = "$MODEL_FOLDER/$file"
                val outFile = File(baseDir, assetPath)
                outFile.parentFile?.mkdirs()

                try {
                    if (!outFile.exists()) {
                        context.assets.open(assetPath).use { input ->
                            FileOutputStream(outFile).use { output -> input.copyTo(output) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "File model nggak ketemu: $assetPath")
                }
            }

            isReady = true
            Log.d(TAG, "Setup selesai, semua file siap!")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Gagal setup: ${e.message}")
            return@withContext false
        }
    }

    suspend fun upscale(context: Context, input: Bitmap, scale: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val baseDir = context.filesDir
            val tmpDir  = context.cacheDir

            val inputFile  = File(tmpDir, "realsr_input.png")
            val outputFile = File(tmpDir, "realsr_output.png")

            val safeBitmap = if (input.config != Bitmap.Config.ARGB_8888) {
                input.copy(Bitmap.Config.ARGB_8888, false)
            } else input

            FileOutputStream(inputFile).use { out ->
                safeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val binaryPath = File(baseDir, "realsr/realsr-ncnn").absolutePath
            val libDir     = File(baseDir, "realsr").absolutePath
            val modelDirPath = File(baseDir, MODEL_FOLDER).absolutePath

            val cmd = arrayOf(
                binaryPath,
                "-i", inputFile.absolutePath,
                "-o", outputFile.absolutePath,
                "-m", modelDirPath,
                "-n", "x$scale", 
                "-s", scale,     
                "-g", "0"
            )

            Log.d(TAG, "Menjalankan: ${cmd.joinToString(" ")}")

            val process = ProcessBuilder(*cmd)
                .apply {
                    val env = environment()
                    val systemLibs = env["LD_LIBRARY_PATH"] ?: ""
                    env["LD_LIBRARY_PATH"] = "$libDir:$systemLibs"
                    redirectErrorStream(true)
                }
                .start()

            // 🔥 BACA LOG DARI MESIN AI-NYA
            val log = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                val result = BitmapFactory.decodeFile(outputFile.absolutePath)
                inputFile.delete()
                outputFile.delete()
                return@withContext result
            } else {
                // 🔥 TULIS LOG KE FOLDER INTERNAL BIAR AMAN DARI BLOKIRAN
                val logDir = context.getExternalFilesDir(null)
                val errorLogFile = File(logDir, "kythera_error_log.txt")
                errorLogFile.writeText("=== GAGAL EKSEKUSI BINARY ===\nEXIT CODE: $exitCode\n\nLOG TERMINAL:\n$log\n")
                Log.e(TAG, "Gagal! Cek log di: ${errorLogFile?.absolutePath}")
                return@withContext null
            }
        } catch (e: Exception) {
            // 🔥 TULIS LOG CRASH
            val logDir = context.getExternalFilesDir(null)
            val errorLogFile = File(logDir, "kythera_error_log.txt")
            errorLogFile.writeText("=== APLIKASI CRASH ===\nPESAN ERROR:\n${e.message}\n\nSTACKTRACE:\n${e.stackTraceToString()}")
            Log.e(TAG, "Error upscale: ${e.message}", e)
            return@withContext null
        }
    }
}
