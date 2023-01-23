package com.ikami.encryptionsample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.VideoView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.material.snackbar.Snackbar
import com.ikami.encryptionsample.databinding.ActivityMainBinding
import com.ikami.encryptionsample.utils.EncryptedFileDataSource
import com.ikami.encryptionsample.utils.EncryptionUtil
import com.ikami.encryptionsample.utils.FileUtil
import java.io.*
import java.lang.reflect.Field
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class MainActivity : AppCompatActivity() {
    private var encryptedVideoFile = ""
    private lateinit var masterKey: MasterKey
    private lateinit var pickVideoLauncher: ActivityResultLauncher<String>
    private lateinit var videoView: VideoView
    private lateinit var encryptedVideoString: String
    private val util = FileUtil()
    private val encryptionUtils = EncryptionUtil()
    private var exoPlayer: SimpleExoPlayer? = null
    private lateinit var mainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        initializeUI()

    }

    private fun showSnackbar(@StringRes messageRes: Int) {
        Snackbar.make(findViewById<Button>(R.id.rootView), messageRes, Snackbar.LENGTH_LONG).show()
    }

    private fun initializeUI() {
        println("initializeUI called ======")


        // Setup video picker launcher
        pickVideoLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { videoUri ->
                videoUri?.let {

                    //encode my video file

                    /*
                    val encodedVideo = Base64.encodeToString(
                        contentResolver.openInputStream(it)?.let { videoUri ->
                            util.getFileBytes(
                                videoUri
                            )
                        }, 0
                    )
                    */

                    //val encodedVideo = encodingToBase64(videoUri)
                        //encrypt the encoded video file
                    contentResolver.openInputStream(it)?.let { it ->
                        encryptedVideoString = encryptionUtils.encryptFile(this, "AAAAAAAAAAAAAAAA",it)
                        /*
                        if (encryptedVideoString.isNotEmpty()){
                            showSnackbar(R.string.success_video_encryption)
                        } else {
                            showSnackbar(R.string.error_unable_to_save_file)
                        }

                        val inputFile = File(it.toString())
                        Log.e("Picked File name is ===>>", "${inputFile.name}")
                        */
                    }


                }
            }

        // Setup SELECT VIDEO button
        mainBinding.buttonSelectVideo.setOnClickListener {
            askForPermissions()
            pickVideoLauncher.launch("video/*")
        }



        mainBinding.buttonDecryptVideo.setOnClickListener {
            val filePath = getDir("uLessonEncryptedVideos", Context.MODE_PRIVATE).toString()
            val fullFilePath = "$filePath/encryptedVideoFile.txt"

            playDecryptedVideo(fullFilePath)

            /*
            val decryptedEncodedVideoString = encryptionUtils.decryptVideo(this, fullFilePath, "AAAAAAAAAAAAAAAA")

            Log.e(this::class.java.simpleName, "decrypt listener")
            //val decryptedEncodedVideoString = encryptionUtils.decryptFile("AAAAAAAAAAAAAAAA",encryptedVideoString)
            if (decryptedEncodedVideoString.isNotEmpty()){
                showSnackbar(R.string.success_video_decryption)

                playDecryptedVideo(decryptedEncodedVideoString)

                /*
                showSnackbar(R.string.success_video_decryption)
                val decodedVideo = Base64.decode(decryptedEncodedVideoString, 0)
                //util.saveFile("decryptedEncodedVideoFile.mp4", Base64.decode(decryptedEncodedVideoString, 0))

                try {
                    val tempDir = File(createVideoDirectory(), "decryptedEncodedVideoFile.mp4")
                    val fileOutputStream = FileOutputStream(tempDir)
                    fileOutputStream.apply {
                        write(decodedVideo)
                        close()
                    }
                    playDecryptedVideo(tempDir.absolutePath)
                }catch (e: Exception){
                    Log.e(this::class.java.simpleName, "Decryption exception: $e")
                }
                */
            } else {
                showSnackbar(R.string.error_unable_to_decrypt)
            }
            */

        }
    }

    private fun encodingToBase64(uri: Uri): String {
        val file = File(uri.path)
        val capacity = file.length() / 3 * 4
        val sb = StringBuilder(capacity.toInt())
        var fin: FileInputStream? = null
        try {
            fin = FileInputStream("some.file")
            // Max size of buffer
            val bSize = 3 * 512
            // Buffer
            val buf = ByteArray(bSize)
            // Actual size of buffer
            var len = 0
            while (fin.read(buf).also { len = it } != -1) {
                val encoded: ByteArray = Base64.encode(buf, 0)
                sb.append(String(buf, 0, len))
            }
        } catch (e: IOException) {
            fin?.close()
        }
        return sb.toString()
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

    private fun deleteTempFolder() {
        val tempFolder = getVideoDirectory()
        if (tempFolder.exists()){
            Log.e(this::class.java.simpleName, "delete temp folder")
            tempFolder.deleteRecursively()
        } else {
            Log.e(this::class.java.simpleName, "temp folder not found")
        }
    }

    override fun onStop() {
        deleteTempFolder()
        mainBinding.videoPlayer.player = null
        exoPlayer?.release()
        exoPlayer = null
        super.onStop()
    }

    private fun playDecryptedVideo(absolutePath: String) {
        val secretKeySpec = SecretKeySpec("AAAAAAAAAAAAAAAA".toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val fileDataSource = EncryptedFileDataSource(cipher, object : TransferListener {
            override fun onTransferInitializing(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) {
            }

            override fun onTransferStart(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) {
            }

            override fun onBytesTransferred(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean,
                bytesTransferred: Int
            ) {
            }

            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
            }
        })

        val dataSpec = DataSpec(Uri.parse(absolutePath))
        fileDataSource.open(dataSpec)

        val dataSourceFactory = DataSource.Factory {
            fileDataSource
        }

        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(fileDataSource.uri!!))

        exoPlayer = SimpleExoPlayer.Builder(this).build().apply {

            //setMediaItem(MediaItem.fromUri(absolutePath))
            setMediaSource(videoSource)
            prepare()
            playWhenReady = true
        }
        mainBinding.videoPlayer.player = exoPlayer
    }

    private fun askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                return
            }
            createDir()
        }
    }

    private fun createVideoDirectory(): File {
        val directory = getVideoDirectory()
        if (!directory.exists()){
            directory.mkdir()
        }
        return directory
    }

    private fun getVideoDirectory() = getDir("uLessonEncryptedVideos", Context.MODE_PRIVATE)

   private fun createDir() {
        val wallpaperDirectory= File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) ,
            FileUtil.DOCUMENT_FILE_DIRECTORY
        )
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }
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
