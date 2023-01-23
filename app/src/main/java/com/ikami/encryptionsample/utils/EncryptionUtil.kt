package com.ikami.encryptionsample.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec

class EncryptionUtil {

    open fun encryptFile(context: Context, encryptionKey: String, base64Video: InputStream): String {
        println("encryptFile called ======")
        var encryptedVideoString: String =""
         try {
            val keyGenerator = KeyGenerator.getInstance("AES")
            val secureRandom = SecureRandom()
            val keyBitSize = 256
            keyGenerator.init(keyBitSize, secureRandom)
            val secretKey = keyGenerator.generateKey()

            val secretKeySpec = SecretKeySpec(encryptionKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
             val inputBytes = ByteArray(base64Video.available())
             Log.e(this::class.java.simpleName, "inputBytes size: ${inputBytes.size}")
             //base64Video.read(inputBytes)
            // val cipherText = cipher.doFinal(inputBytes)
             saveEncryptedVideo(context, cipher, base64Video)

             //encryptedVideoString = Base64.encodeToString(cipherText, Base64.DEFAULT)
            //val cipherText = cipher.doFinal(base64Video.toByteArray())
        } catch (e: java.security.NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: javax.crypto.NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: java.security.InvalidKeyException) {
            e.printStackTrace()
        } catch (e: javax.crypto.BadPaddingException) {
            e.printStackTrace()
        } catch (e: javax.crypto.IllegalBlockSizeException) {
            e.printStackTrace()
        }
        return encryptedVideoString
    }

    open fun decryptFile(encryptionKey: String, encryptedBase64Video: String): String {

        println("decryptFile called ======")
        var decryptedVideoString =""
        try {

            val secretKeySpec = SecretKeySpec(encryptionKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)

            val decodedValue = Base64.decode(encryptedBase64Video.toByteArray(), Base64.DEFAULT)
            val values = cipher.doFinal(decodedValue)
             decryptedVideoString = String(values)
        } catch (e: Exception){
            Log.e(this::class.java.simpleName, "decrpty exception: $e")
            e.printStackTrace()
        }
        println("decryptedVideoString is = ${decryptedVideoString.toCharArray().size}")
        return decryptedVideoString
    }

    private fun saveEncryptedVideo(
        context: Context,
        cipher: Cipher,
        base64Video: InputStream
    ){
        val videoFileDirectory = context.getDir("uLessonEncryptedVideos", Context.MODE_PRIVATE)
        try {
            val fileObject = File(videoFileDirectory, "encryptedVideoFile.txt")
            fileObject.createNewFile()


            val fo = FileOutputStream(fileObject)
            val bufferedOutputStream = BufferedOutputStream(fo)

            val cipherOutputStream = CipherOutputStream(bufferedOutputStream, cipher)
            //val byteArray = inputStreamToByteArray(base64Video)
            val byteArray = ByteArray(base64Video.available())
            base64Video.read(byteArray)

            //val byteArray = outputStream?.toByteArray()
            //Log.e(this::class.java.simpleName, "byte array size: ${byteArray!!.size}")

            byteArray.let {
                cipherOutputStream.write(it)
                cipherOutputStream.flush()
                cipherOutputStream.close()
            }
            //fo.write(content)
            //fo.close()
            Log.e(this::class.java.simpleName, "Encrypted successfully")
            Toast.makeText(context, "file encrypted with success", Toast.LENGTH_SHORT).show()
        }catch (e: Exception){
            e.printStackTrace()
            Log.e(this::class.java.simpleName, "encryption exception: $e")
            Toast.makeText(context, "file encryption failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inputStreamToByteArray(inputStream: InputStream): ByteArray? {
        return try {
            val outputStream = ByteArrayOutputStream()
            val bufferSize = inputStream.available()
            var count = 0L
            outputStream.use { output ->
                inputStream.use { input ->
                    val buffer = ByteArray(bufferSize)
                    while (true){
                        val byteCount = input.read(buffer)
                        if (byteCount < 0) break
                        output.write(buffer, 0, byteCount)
                        count += byteCount
                    }
                    output.flush()
                    output.close()
                }
            }

            val overAllCount: Int = if (count > Integer.MAX_VALUE) {
                 -1
            } else count.toInt()



            Log.e(this::class.java.simpleName, "inputStreamToByteArray")
            /*
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output, bufferSize)
                }
            }
            */
            outputStream.toByteArray()
        } catch (e: Exception){
            Log.e(this::class.java.simpleName, "inputStreamToByteArray exception: $e")
            null
        }
    }


    fun decryptVideo(context: Context, absoluteFilePath: String, secretKey: String): String {
        return try {
            val initialFile = File(absoluteFilePath)
            val fileInputStream = FileInputStream(initialFile)
            val inputStream = BufferedInputStream(fileInputStream)

            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)

            val inputBytes = ByteArray(inputStream.available())
            inputStream.read(inputBytes)
            val outputBytes = cipher.doFinal(inputBytes)

            val videoFileDirectory = context.getDir("uLessonEncryptedVideos", Context.MODE_PRIVATE)
            val fileObject = File(videoFileDirectory, "decryptedVideoFile.mp4")
            fileObject.createNewFile()
            val fo = FileOutputStream(fileObject)
            fo.write(outputBytes)
            fo.close()
            inputStream.close()
            fileObject.absolutePath
        } catch (e: Exception){
            Log.e(this::class.java.simpleName, "decryptVideo exception: $e")
            ""
        }
    }
}