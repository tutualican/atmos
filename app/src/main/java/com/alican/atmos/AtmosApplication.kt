package com.alican.atmos

import android.app.Application
import com.alican.atmos.data.AtmosStore
import com.alican.atmos.i18n.Loc

class AtmosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the stored interface language before any activity or widget renders.
        runCatching {
            val settings = AtmosStore(this).settings
            Loc.apply(settings.language)
            com.alican.atmos.ui.theme.Atmos.applySystemAccent(this, settings.systemAccent)
        }
    }
}
