package com.d4nzxml.kythera.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object RealSrEngine {

    private const val TAG = "Kythera_AI"
    private var isReady = false

    // ─── Binary tetap dari folder realsr/ ───
    private val BINARIES = listOf(
        "realsr/realsr-ncnn",
        "realsr/libncnn.so",
        "realsr/libc++_shared.so",
        "realsr/libomp.so"
    )

    // ─── Daftar folder model sesuai isi assets lu ───
    private val MODEL_FOLDERS = listOf(
        "models-Real-ESRGANv3-anime",
        "models-Real-ESRGAN-anime",
        "models-Real-ESRGAN",
        "models-pro",
        "models-ESRGAN-Nomos8kSC"
    )

    // Karena lu pakai scale 4x, kita fokus narik file x4.bin dan x4.param aja dari tiap folder
    private val MODEL_FILES = listOf("x4.bin", "x4.param")

    // ─── Setup: copy file dari assets ke filesDir ───────────────────────
    suspend fun setup(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isReady) return@withContext true

        try {
            val baseDir = context.filesDir

            // 1. Copy binary + lib
            for (assetPath in BINARIES) {
                val outFile = File(baseDir, assetPath)
                outFile.parentFile?.mkdirs()
                
                // Pengecekan aman pakai try-catch
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

            // 2. Copy file x4 dari masing-masing folder model
            for (folder in MODEL_FOLDERS) {
                for (file in MODEL_FILES) {
                    val assetPath = "$folder/$file"
                    val outFile = File(baseDir, assetPath)
                    outFile.parentFile?.mkdirs()
                    
                    try {
                        if (!outFile.exists()) {
                            context.assets.open(assetPath).use { input ->
                                FileOutputStream(outFile).use { output -> input.copyTo(output) }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "File model nggak ketemu, skip: $assetPath")
                    }
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

    // ─── Main function: upscale bitmap ────────────────────────────────────────
    suspend fun upscale(context: Context, input: Bitmap, modelFolder: String): Bitmap? = withContext(Dispatchers.IO) {
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
            
            // 🔥 INI KUNCINYA: Arahin parameter -m ke folder yang dipilih user
            val modelDirPath = File(baseDir, modelFolder).absolutePath

            // Perintah shell: -m ngarah ke folder spesifik, -n ngarah ke "x4"
            val cmd = arrayOf(
                binaryPath,
                "-i", inputFile.absolutePath,
                "-o", outputFile.absolutePath,
                "-m", modelDirPath,
                "-n", "x4",
                "-s", "4",
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

            val log = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            Log.d(TAG, "Exit code: $exitCode")
            Log.d(TAG, "Log: $log")

            if (exitCode == 0 && outputFile.exists()) {
                val result = BitmapFactory.decodeFile(outputFile.absolutePath)
                inputFile.delete()
                outputFile.delete()
                return@withContext result
            } else {
                val logDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kythera")
                if (!logDir.exists()) logDir.mkdirs()
                
                val errorLogFile = File(logDir, "kythera_error_log.txt")
                errorLogFile.writeText("=== GAGAL EKSEKUSI BINARY ===\nEXIT CODE: $exitCode\n\nLOG TERMINAL:\n$log\n")
                
                return@withContext null
            }

        } catch (e: Exception) {
            val logDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kythera")
            if (!logDir.exists()) logDir.mkdirs()
            
            val errorLogFile = File(logDir, "kythera_error_log.txt")
            errorLogFile.writeText("=== APLIKASI CRASH ===\nPESAN ERROR:\n${e.message}\n\nSTACKTRACE:\n${e.stackTraceToString()}")
            
            return@withContext null
        }
    }
}
