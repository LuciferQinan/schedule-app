package com.heqinan.schedule.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 修改这里为你的 NAS IP
    private const val BASE_URL = "http://192.168.50.163:8080/"
    
    val taskApi: TaskApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApi::class.java)
    }
}
