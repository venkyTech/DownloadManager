package com.download.downloadmanager

import android.app.ProgressDialog;
import android.content.Context;
import android.provider.SyncStateContract.Helpers.update
import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;


class DownloadUtils {

    var fileName: String = ""
    var fileUrl: String = ""
    var mContext: Context? = null

    var root = android.os.Environment.getExternalStorageDirectory()
    var downloadDir = File(root.absolutePath + "/androidfeatures/") //it is my root directory
    var progress: ProgressDialog? = null
    val totalProgressTime = 100

    /**
     * This method will start downloading the file
     */
    fun startDownload() {
       // progress!!.show()
        Log.wtf("SKDINFO", "Download started")
        //setup the dirs
        val tmpFile = File(downloadDir.absolutePath + "/" + fileName)
        if (downloadDir.exists() == false) {
            downloadDir.mkdirs()
            Log.wtf("SKDINFO", "folder created")
        }

        //setup the request
        val request = Request.Builder()
                .url(fileUrl)
                .build()

        //setup the progressListner
        val progressListener = object : ProgressListener {
            internal var firstUpdate = true

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                if (done) {
                    Log.wtf("SKDINFO", "Download Complete")

                } else {

                    if (firstUpdate) {
                        firstUpdate = false
                        if (contentLength.toInt() == -1) {
                            Log.wtf("SKDINFO", "content-length: unknown")
                        } else {
                            Log.wtf("SKDINFO", "content-length: $contentLength")
                        }
                    }


                    if (contentLength.toInt() != -1) {
                        Log.wtf("SKDINFO", "" + 100 * bytesRead / contentLength)
//                        progress!!.progress = (100 * bytesRead / contentLength).toInt()
                    }
                }
            }
        }


        //init okHttp client
        val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addNetworkInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    originalResponse.newBuilder()
                            .body(ProgressResponseBody(originalResponse.body()!!, progressListener))
                            .build()
                }
                .build()

        //send the request and write the file

        Log.wtf("SKDINFO", "Download Starting")

        Thread(Runnable {
            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                //download Success now
                Log.wtf("SKDINFO", "Download Completed")
                //FileUtils.copyInputStreamToFile(response.body()!!.byteStream(), tmpFile)
                response.close() //close reponse to avoid memory leak
//                progress!!.dismiss()
            } catch (e: IOException) {
                e.printStackTrace()
               // progress!!.dismiss()

            }
        }).start()


    }

    constructor(context: Context, fileUrl: String, fileName: String) {
        this.fileUrl = fileUrl
        this.fileName = fileName
        this.mContext = context
//        progress = ProgressDialog(context)
//        progress!!.setMessage("Downloading form ")
//        progress!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
//        progress!!.isIndeterminate = false
//        progress!!.setCancelable(false)

        Log.wtf("SKDINFO", "DownloadUtils$fileName")

    }

    /**
     * custom response body
     */
    private class ProgressResponseBody internal constructor(private val responseBody: ResponseBody, private val progressListener: ProgressListener) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody.contentType()
        }

        override fun contentLength(): Long {
            return responseBody.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()))
            }
            return bufferedSource!!
        }


        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                internal var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += if (bytesRead.toInt() != -1) bytesRead else 0
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead.toInt() == -1)
                    return bytesRead
                }
            }
        }
    }

    /**
     * Listner
     */
    internal interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }


}