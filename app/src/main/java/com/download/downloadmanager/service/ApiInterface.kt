package com.download.downloadmanager.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiInterface {

    @GET("war-stable/latest/jenkins.war")
    @Streaming
    fun  downloadFile() : Call <ResponseBody>
}