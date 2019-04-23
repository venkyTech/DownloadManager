package com.download.downloadmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.download.downloadmanager.worker.DownloadWork


class HomeActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(checkPermission()){
            startDownload();
        } else {
            requestPermission();
        }
    }

    private fun startDownload() {


        val dowloadWork = OneTimeWorkRequestBuilder<DownloadWork>().build()

        var workManager=WorkManager.getInstance().beginWith(listOf(dowloadWork,dowloadWork,dowloadWork))
                .enqueue()
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return if (result == PackageManager.PERMISSION_GRANTED) {

            true

        } else {

            false
        }
    }

    private fun requestPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startDownload()
            } else {

                Toast.makeText(
                    baseContext,
                    "Permission Denied, Please allow to proceed !",
                    Toast.LENGTH_LONG
                ).show()

            }
        }
    }
}
