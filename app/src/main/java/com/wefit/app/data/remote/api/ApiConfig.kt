package com.wefit.app.data.remote.api

object ApiConfig {

    // Android Emulator -> host machine
    const val BASE_URL_EMULATOR = "http://10.0.2.2:8000/api/"

    // Physical Android device -> PC on same Wi-Fi
    const val BASE_URL_DEVICE = "http://192.168.18.5:8000/api/"

    // Currently using physical device
    const val BASE_URL = BASE_URL_DEVICE
}