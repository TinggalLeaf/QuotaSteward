package com.github.tinggalleaf.ai_quota_dashboard

import android.app.Application

class AIQuotaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
