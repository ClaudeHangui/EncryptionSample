package com.ikami.encryptionsample.utils

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.TransferListener
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import kotlin.math.min


class EncryptedFileDataSource(
    private val cipher: Cipher,
    private var transferListener: TransferListener
): DataSource {

    private var mInputStream: StreamingCipherInputStream? = null
    private var mUri: Uri? = null
    private var mBytesRemaining: Long = 0
    private var mOpened = false


    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        } else if (mBytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }
        // constrain the read length and try to read from the cipher input stream
        // constrain the read length and try to read from the cipher input stream
        val bytesToRead = getBytesToRead(length)
        var bytesRead = -1
        mInputStream?.let {
            bytesRead = try {
                it.read(buffer, offset, bytesToRead)
            }catch (e: IOException) {
                throw EncryptedFileDataSourceException(e)
            }
        }

        // if we get a -1 that means we failed to read - we're either going to EOF error or broadcast EOF
        // if we get a -1 that means we failed to read - we're either going to EOF error or broadcast EOF
        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw EncryptedFileDataSourceException(EOFException())
            }
            return C.RESULT_END_OF_INPUT
        }
        // we can't decrement bytes remaining if it's just a flag representation (as opposed to a mutable numeric quantity)
        // we can't decrement bytes remaining if it's just a flag representation (as opposed to a mutable numeric quantity)
        if (mBytesRemaining != C.LENGTH_UNSET.toLong()) {
            mBytesRemaining -= bytesRead.toLong()
        }
        // notify
        // notify
        mUri?.let { DataSpec(it) }
            ?.let { transferListener.onBytesTransferred(this, it, false, bytesRead) }

        // report
        // report
        return bytesRead
    }

    override fun addTransferListener(transferListener: TransferListener) {}


    override fun open(dataSpec: DataSpec): Long {
        if (mOpened) {
            return mBytesRemaining
        }
        // #getUri is part of the contract...
        mUri = dataSpec.uri
        // put all our throwable work in a single block, wrap the error in a custom Exception
        try {
            setupInputStream()
            skipToPosition(dataSpec)
            computeBytesRemaining(dataSpec)
        } catch (e: IOException) {
            throw EncryptedFileDataSourceException(e)
        }
        // if we made it this far, we're open
        mOpened = true
        // notify
        transferListener.onTransferStart(this, dataSpec, false)
        // report
        return mBytesRemaining
    }

    override fun getUri(): Uri? {
        return mUri
    }

    override fun close() {
        try {
            mInputStream?.close()
            if (mOpened) {
                mOpened = false
                mUri?.let { DataSpec(it) }?.let { transferListener.onTransferEnd(this, it, false) }
                mUri = null
                mInputStream = null
            }
        } catch (e: IOException) {
            throw EncryptedFileDataSourceException(e)
        }
    }

    internal class EncryptedFileDataSourceException(cause: IOException) : IOException(cause)

    private fun getBytesToRead(bytesToRead: Int): Int {
        return if (mBytesRemaining == C.LENGTH_UNSET.toLong()) {
            bytesToRead
        } else min(mBytesRemaining, bytesToRead.toLong()).toInt()
    }


    @Throws(IOException::class)
    private fun computeBytesRemaining(dataSpec: DataSpec) {
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            mBytesRemaining = dataSpec.length
        } else {
            mInputStream?.let {
                mBytesRemaining = it.available().toLong()
            }
            if (mBytesRemaining.toInt() == Int.MAX_VALUE) {
                mBytesRemaining = C.LENGTH_UNSET.toLong()
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun setupInputStream() {
        val encryptedFile = mUri?.path?.let { File(it) }
        val fileInputStream = FileInputStream(encryptedFile)
        val inputStream = BufferedInputStream(fileInputStream)

        mInputStream = StreamingCipherInputStream(inputStream, cipher)
    }

    @Throws(IOException::class)
    private fun skipToPosition(dataSpec: DataSpec) {
        mInputStream?.forceSkip(dataSpec.position)
    }

    internal class StreamingCipherInputStream(inputStream: InputStream, cipher: Cipher) : CipherInputStream(inputStream, cipher) {
        private var mBytesAvailable = 0

        init {
            mBytesAvailable = inputStream.available()
        }

        @Throws(IOException::class)
        fun forceSkip(bytesToSkip: Long): Long {
            var processedBytes: Long = 0
            while (processedBytes < bytesToSkip) {
                var bytesSkipped = skip(bytesToSkip - processedBytes)
                if (bytesSkipped == 0L) {
                    if (read() == -1) {
                        throw EOFException()
                    }
                    bytesSkipped = 1
                }
                processedBytes += bytesSkipped
            }
            return processedBytes
        }

        override fun available(): Int {
            return mBytesAvailable
        }
    }
}