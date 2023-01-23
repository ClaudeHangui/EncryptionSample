package com.ikami.encryptionsample.utils

import android.os.Environment
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Field


class FileUtil {

    @Throws(IOException::class)
    open fun getFileBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()

        /*
        val fullBufferSize = length(inputStream, 1024)

        return try {
            copy(inputStream, byteBuffer)
            byteBuffer.toByteArray()
        }catch (e: Exception){
            Log.e(this::class.java.simpleName, "toByteArray exception: $e")
            null
        }
        */


        val bufferSize = 1024 * 4
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }


        return byteBuffer.toByteArray()
    }

    @Throws(IOException::class)
    fun length(inputStream: InputStream, chunkSize: Int): Int {
        val buffer = ByteArray(chunkSize)
        var chunkBytesRead = 0
        var length = 0
        while (inputStream.read(buffer).also { chunkBytesRead = it } != -1) {
            length += chunkBytesRead
        }
        return length
    }

    fun getInputLength(inputStream: InputStream?): Long {
        try {
            if (inputStream is FilterInputStream) {
                val field: Field = FilterInputStream::class.java.getDeclaredField("in")
                field.isAccessible = true
                val internal = field.get(inputStream) as InputStream
                return getInputLength(internal)
            } else if (inputStream is ByteArrayInputStream) {
                val field: Field = ByteArrayInputStream::class.java.getDeclaredField("buf")
                field.isAccessible = true
                val buffer = field.get(inputStream) as ByteArray
                return buffer.size.toLong()
            } else if (inputStream is FileInputStream) {
                return inputStream.channel.size()
            }
        } catch (exception: NoSuchFieldException) {
            // Ignore all errors and just return -1.
            exception.printStackTrace()
        } catch (exception: IllegalAccessException) {
            exception.printStackTrace()
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
        return -1
    }

    @Throws(IOException::class)
    fun copy(input: InputStream?, output: OutputStream): Int {
        val count = copyLarge(input!!, output)
        return if (count > Int.MAX_VALUE) {
            -1
        } else count.toInt()
    }

    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var count: Long = 0
        var n = 0
        while (-1 != input.read(buffer).also { n = it }) {
            output.write(buffer, 0, n)
            count += n.toLong()
        }
        return count
    }

    open fun saveFile(filename: String?, content: ByteArray?): String? {

        println("Save file is called!!!!!");
        val bytes = ByteArrayOutputStream()
        val wallpaperDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            DOCUMENT_FILE_DIRECTORY
        )

        return try {
            println("Writing file in the local storage called!!!!!");

            val f = File(wallpaperDirectory, filename)
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(content)
            fo.close()
            f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
            ""
        }
    }


    companion object {


        const val DOCUMENT_FILE_DIRECTORY = "/POC/EncryptionFiles"

    }
}