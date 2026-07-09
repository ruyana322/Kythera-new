package com.d4nzxml.kythera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object RealSrEngine {
    private const val TAG = "Kythera_RealSR"
    private var isInitialized = false

    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "realsr")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            copyAssetToCache(context, "realsr/realsr-ncnn", cacheDir)
            val binaryFile = File(cacheDir, "realsr-ncnn")
            binaryFile.setExecutable(true)

            copyAssetToCache(context, "realsr/libc++_shared.so", cacheDir)
            copyAssetToCache(context, "realsr/libncnn.so", cacheDir)
            copyAssetToCache(context, "realsr/libomp.so", cacheDir)

            copyModelFiles(context, cacheDir)

            isInitialized = true
            Log.d(TAG, "RealSR initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RealSR: ${e.message}")
            false
        }
    }

    private fun copyAssetToCache(context: Context, assetPath: String, cacheDir: File) {
        val fileName = assetPath.substringAfterLast("/")
        val outFile = File(cacheDir, fileName)
        if (!outFile.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun copyModelFiles(context: Context, cacheDir: File) {
        val modelsDir = File(cacheDir, "models-Real-ESRGAN")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        listOf("x4.param", "x4.bin").forEach { fileName ->
            val outFile = File(modelsDir, fileName)
            if (!outFile.exists()) {
                context.assets.open("realsr/models-Real-ESRGAN/$fileName").use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    suspend fun upscaleImage(
        context: Context,
        inputBitmap: Bitmap,
        scale: Int = 4
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext null

        try {
            val maxDimension = 2048
            val scaledBitmap = if (inputBitmap.width > maxDimension || inputBitmap.height > maxDimension) {
                val scaleFactor = maxDimension.toFloat() / maxOf(inputBitmap.width, inputBitmap.height)
                Bitmap.createScaledBitmap(inputBitmap, (inputBitmap.width * scaleFactor).toInt(), (inputBitmap.height * scaleFactor).toInt(), true)
            } else inputBitmap

            val inputDir = File(context.cacheDir, "realsr_input").apply { mkdirs() }
            val outputDir = File(context.cacheDir, "realsr_output").apply { mkdirs() }
            val inputFile = File(inputDir, "input.png")
            val outputFile = File(outputDir, "output.png")

            FileOutputStream(inputFile).use { out -> scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

            val cacheDir = File(context.cacheDir, "realsr")
            val command = listOf(
                File(cacheDir, "realsr-ncnn").absolutePath,
                "-i", inputFile.absolutePath,
                "-o", outputFile.absolutePath,
                "-m", File(cacheDir, "models-Real-ESRGAN").absolutePath,
                "-s", scale.toString(),
                "-t", "4"
            )

            Log.d(TAG, "Executing: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(cacheDir)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "Exit code: $exitCode, Output: $output")

            if (exitCode == 0 && outputFile.exists()) {
                val result = BitmapFactory.decodeFile(outputFile.absolutePath)
                inputFile.delete()
                outputFile.delete()
                result
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            null
        }
    }
}
