package com.wefit.app.data.remote.api

object ApiConfig {

    // Android Emulator -> host machine (kept for local dev reference only)
    const val BASE_URL_EMULATOR = "http://10.0.2.2:8000/api/"

    // Physical Android device -> PC on same Wi-Fi (kept for local dev reference only)
    const val BASE_URL_DEVICE = "http://192.168.18.5:8000/api/"

    // Production — live Railway backend
    const val BASE_URL_PRODUCTION = "https://wefit-backend-production.up.railway.app/api/"

    // Currently using production
    const val BASE_URL = BASE_URL_PRODUCTION
}