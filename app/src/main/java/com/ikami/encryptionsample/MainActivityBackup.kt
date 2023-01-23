package com.ikami.encryptionsample

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKeys
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.google.crypto.tink.streamingaead.StreamingAeadFactory
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates.AES128_CTR_HMAC_SHA256_4KB
import java.io.*
import java.security.GeneralSecurityException


class MainActivityBackup : AppCompatActivity() {
    private var encryptedVideoFile = ""
    private lateinit var masterKey: MasterKey
    private lateinit var pickVideoLauncher: ActivityResultLauncher<String>
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()

    }


    private fun decryptVideo(streamingAead: StreamingAead, associatedData: ByteArray,uri: Uri) {


        println("decryptVideo method called ======")

        val inputFile = File(uri.toString())
        val outputFile = File.createTempFile("decrypted", ".mp4")

        println("inputFile path ====== ${inputFile.absolutePath}")
        println("inputFile name ====== ${inputFile.name}")

        val ciphertextStream: InputStream = streamingAead.newDecryptingStream(FileInputStream(inputFile), associatedData)
        val plaintextStream: OutputStream = FileOutputStream(outputFile)
        val chunk = ByteArray(1024)
        var chunkLen = 0
        while (ciphertextStream.read(chunk).also { chunkLen = it } != -1) {
            plaintextStream.write(chunk, 0, chunkLen)
        }
        ciphertextStream.close();
        plaintextStream.close();
    }

    private fun initializeUI() {
        println("initializeUI called ======")

        // Setup SELECT VIDEO button
        findViewById<Button>(R.id.button_select_video).setOnClickListener {
        //    pickVideoLauncher.launch("video/*")
            val filePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "two_encrypted-1.mp4").toUri()

            try {
                StreamingAeadConfig.register()

                // Read the keyset into a KeysetHandle
                var handle: KeysetHandle? = null
                handle = KeysetHandle.generateNew(AES128_CTR_HMAC_SHA256_4KB)
                setup(handle,filePath )
            } catch (e: GeneralSecurityException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


        // Setup video picker launcher
        pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { videoUri ->
                videoUri?.let {

                    println("The uri path ${it.toString()}")
                    println("The uri path ${it.path}")
                    val uniqueName = getFileNameFromUri(it)
                    println("the file unique name is $uniqueName")

                }
            }



//
//        // Initialize MasterKey
//        // Todo: Handle APILevel below 23
//        val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
//        //val keyGenParameterSpec = MasterKeys.AES128_CTR_HMAC_SHA256_4KB
//        val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
//        masterKey = MasterKey.Builder(applicationContext)
//            .setKeyGenParameterSpec(keyGenParameterSpec)
//            .build()
//
//        println("The master key is ======${masterKey}")
//        // Setup video picker launcher
//        pickVideoLauncher =
//            registerForActivityResult(ActivityResultContracts.GetContent()) { videoUri ->
//                videoUri?.let {
//                    saveEncryptedVideo(it)
////                    val encodedVideo = Base64.encodeToString(
////                        Utils.getBytes(
////                            contentResolver.openInputStream(it)
////                        ), 0
//                    // )
//                }
//            }
//
//        // Setup SELECT VIDEO button
//        findViewById<Button>(R.id.button_select_video).setOnClickListener {
//            pickVideoLauncher.launch("video/*")
//        }
//
//        // Setup DECRYPT VIDEO button
//        findViewById<Button>(R.id.button_decrypt_video).setOnClickListener {
//            readFile()
//        }
//        videoView = findViewById(R.id.videoViewLyt)

    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun setup(handle: KeysetHandle?,uri: Uri) {
        println("setup method called ======")
        // Get the primitive
        var streamingAead: StreamingAead? = null
        try {
            streamingAead = StreamingAeadFactory.getPrimitive(handle)
        } catch (ex: GeneralSecurityException) {
            System.err.println("Cannot create primitive, got error: $ex")
            System.exit(1)
        }
        val associatedData = ByteArray(0)
        streamingAead?.let { decryptVideo(it, associatedData,uri) }
    }


    /**
     * Save video as encrypted file from [videoUri].
     */
    private fun saveEncryptedVideo(videoUri: Uri) {
        val uniqueName = getFileNameFromUri(videoUri)
        println("the file unique name is $uniqueName")
        encryptedVideoFile = "encrypted_video_$uniqueName.txt"
        val root = Environment.getExternalStorageDirectory().toString()

        deleteFileIfExist(encryptedVideoFile, root)
        println("saveEncryptedVideo method called $videoUri")
        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            File(filesDir, encryptedVideoFile),
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val videoInputStream = contentResolver.openInputStream(videoUri)
        writeFile(encryptedFile.openFileOutput(), videoInputStream)
    }

    /**
     * Delete file with name [filename] in internal storage if exist.
     */
    private fun deleteFileIfExist(filename: String, storagePath: String) {
        val file = File(filesDir, filename)
        // val file = File(storagePath, filename)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Write [inputStream] into [outputStream].
     */
    private fun writeFile(outputStream: FileOutputStream, inputStream: InputStream?) {
        println("writeFile method called ++++++++++++==============")
        outputStream.use { output ->
            inputStream.use { input ->
                input?.let {
                    val buffer =
                        ByteArray(4 * 1024) // buffer size
                    while (true) {
                        val byteCount = input.read(buffer)
                        if (byteCount < 0) break
                        output.write(buffer, 0, byteCount)
                    }
                    output.flush()
                }
            }
        }
    }

    private fun readFile() {
        println("readFile method called ++++++++++++==============")
        var encryptedVideoFilePath = "encrypted_video_cat_four.mp4.txt"
        val encryptedFile = EncryptedFile.Builder(
            applicationContext,
            File(filesDir, encryptedVideoFilePath),
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val inputStream = encryptedFile.openFileInput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        var nextByte: Int = inputStream.read()
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte)
            nextByte = inputStream.read()
        }

        val plainVideo: ByteArray = byteArrayOutputStream.toByteArray()
        convertBytesToFile(plainVideo)

    }

    @SuppressLint("Range")
    private fun getFileNameFromUri(uri: Uri): String {
        // return File(uri.path).name

        var result = ""
        if (uri.scheme.equals("content")) {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor.use { it ->
                if (it != null && it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return result

    }

    private fun convertBytesToFile(bytearray: ByteArray) {
        try {
            val outputFile = File.createTempFile("decrypted_video", ".mp4", cacheDir)
            outputFile.deleteOnExit()
            val fileOutputStream = FileOutputStream("decrypted_videos")
            fileOutputStream.write(bytearray)
            fileOutputStream.close()


        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

}
