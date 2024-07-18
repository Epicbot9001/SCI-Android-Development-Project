package edu.gatech.ce.allgather.utils

import android.os.Build
import androidx.annotation.RequiresApi
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


fun isInteger(input: String): Boolean {
    return input.toIntOrNull() != null
}

@RequiresApi(Build.VERSION_CODES.O)
fun zipFolder(folderPath: String, zipFilePath: String) {
    val p = Paths.get(zipFilePath)
    Files.createDirectories(p.parent)

    val parentDir = File(folderPath).parentFile

    ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(zipFilePath))).use { zipOut ->
        val sourceDir = File(folderPath)
        val files = sourceDir.walkTopDown().filter { it.isFile }.toList()

        for (file in files) {
            val zipEntry = ZipArchiveEntry(file.relativeTo(parentDir).path)
            zipOut.putArchiveEntry(zipEntry)
            FileInputStream(file).use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeArchiveEntry()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun zipFolderNoMp4(folderPath: String, zipFilePath: String) {
    val p = Paths.get(zipFilePath)
    Files.createDirectories(p.parent)

    val parentDir = File(folderPath).parentFile

    ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(zipFilePath))).use { zipOut ->
        val sourceDir = File(folderPath)
        val files = sourceDir.walkTopDown().filter { it.isFile }.toList()

        //filter out nonmp4 files
        val nonMp4files = files.filter{it.extension != "mp4"}

        for (file in nonMp4files) {
            val zipEntry = ZipArchiveEntry(file.relativeTo(parentDir).path)
            zipOut.putArchiveEntry(zipEntry)
            FileInputStream(file).use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeArchiveEntry()

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun unzip(file: File, deleteZip: Boolean = true) {
    ZipArchiveInputStream(BufferedInputStream(FileInputStream(file))).use { zipIn ->
        var entry = zipIn.nextEntry
        while (entry != null) {
            val filePath = Paths.get(file.parent, entry.name)
            Files.createDirectories(filePath.parent)
            FileOutputStream(filePath.toFile()).use { output ->
                zipIn.copyTo(output)
            }
            entry = zipIn.nextEntry
        }

        if (deleteZip) {
            file.delete()
        }
    }
}

fun toMatrix(data: FloatArray): Array<DoubleArray> {
    return arrayOf(data.map { it.toDouble() }.toDoubleArray())
}

fun toFloatArray(data: Array<DoubleArray>): FloatArray {
    return data[0].map { it.toFloat() }.toFloatArray()
}
