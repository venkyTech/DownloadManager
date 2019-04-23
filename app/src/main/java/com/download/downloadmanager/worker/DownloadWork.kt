package com.download.downloadmanager.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.NotificationManager
import android.R
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat.getSystemService
import retrofit2.Retrofit
import java.io.IOException
import android.R.string.cancel
import android.content.Intent
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v4.content.LocalBroadcastManager
import androidx.work.Data
import com.download.downloadmanager.model.Download
import com.download.downloadmanager.service.ApiInterface
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import javax.xml.datatype.DatatypeConstants.SECONDS
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.download.downloadmanager.DownloadUtils
import java.util.*


class DownloadWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var totalFileSize: Int = 0

    override fun doWork(): Result {

        //https://www.learn2crack.com/2016/05/downloading-file-using-retrofit.html
//        for (i in 1..100) {
//            Log.e("Work=",""+i)
//
//            if (i == 100) {
//                return Result.success()
//            }else{
//
//            }
//        }

        notificationManager = this.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = NotificationCompat.Builder(this.applicationContext)
            .setSmallIcon(R.mipmap.sym_def_app_icon)
            .setContentTitle("Download")
            .setContentText("Downloading File")
            .setAutoCancel(true)
        notificationManager!!.notify(0, notificationBuilder!!.build())

        initDownload()

        val down = DownloadUtils(applicationContext, "http://mirrors.jenkins.io/war-stable/latest/jenkins.war", "package.zip")
        // DownloadUtils down = new DownloadUtils(mContext,"http://192.168.0.106/test/package.zip","package.zip");
        down.startDownload()


        return Result.success()
    }

    private fun initDownload(){

        val client : OkHttpClient.Builder = OkHttpClient.Builder()
        client.connectTimeout(60, TimeUnit.SECONDS)
        client.readTimeout(60, TimeUnit.SECONDS)
        //client.writeTimeout(60, TimeUnit.SECONDS)
        val retrofit =  Retrofit.Builder()
            .baseUrl("http://mirrors.jenkins.io/")
                .client(client.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitInterface = retrofit.create(ApiInterface::class.java)

        val request = retrofitInterface.downloadFile() as Call
        try {

            downloadFile(request!!.execute()!!.body()!!)

        } catch ( e : IOException) {

            e.printStackTrace();

        }
    }

    @Throws(IOException::class)
    private fun downloadFile(body: ResponseBody) {

        var count: Int
        var data = ByteArray(1024 * 4)
        val fileSize = body.contentLength()
        val bis = BufferedInputStream(body.byteStream(), 1024 * 8)
        val fileName = Calendar.getInstance().time
        val outputFile =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName.toString()+"file.zip")
        val output = FileOutputStream(outputFile)
        var total: Long = 0
        val startTime = System.currentTimeMillis()
        var timeCount = 1

        while (true) {

            count = bis.read(data)

            if (count == -1) {
                break
            }
            total += count.toLong()
            totalFileSize = (fileSize / Math.pow(1024.0, 2.0)).toInt()
            val current = Math.round(total / Math.pow(1024.0, 2.0)).toDouble()

            val progress = (total * 100 / fileSize).toInt()

            val currentTime = System.currentTimeMillis() - startTime

            var download = Download(0,0,0)
            download!!.totalFileSize=totalFileSize

            if (currentTime > 1000 * timeCount) {

                download.currentFileSize=current.toInt()
                download.progress=progress
                sendNotification(download)
                timeCount++
            }

            output.write(data, 0, count)
        }
        onDownloadComplete()
        output.flush()
        output.close()
        bis.close()

    }

    private fun sendNotification(download: Download) {

        sendIntent(download)
        notificationBuilder!!.setProgress(100, download.progress, false)
        notificationBuilder!!.setContentText("Downloading file " + download.currentFileSize + "/" + totalFileSize + " MB")
        notificationManager!!.notify(0, notificationBuilder!!.build())
    }

    private fun sendIntent(download: Download) {

//        val intent = Intent(MainActivity.MESSAGE_PROGRESS)
//        intent.putExtra("download", download)
//        LocalBroadcastManager.getInstance(this@DownloadService).sendBroadcast(intent)
    }

    private fun onDownloadComplete() {

        val download = Download(0,0,0)
        download.progress=(100)
        sendIntent(download)

        notificationManager!!.cancel(0)
        notificationBuilder!!.setProgress(0, 0, false)
        notificationBuilder!!.setContentText("File Downloaded")
        notificationManager!!.notify(0, notificationBuilder?.build())

    }

    fun onTaskRemoved(rootIntent: Intent) {
        notificationManager!!.cancel(0)
    }
}