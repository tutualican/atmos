package com.alican.atmos

import android.app.Application
import com.alican.atmos.data.AtmosStore
import com.alican.atmos.i18n.Loc

class AtmosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply the stored interface language before any activity or widget renders.
        runCatching { Loc.apply(AtmosStore(this).settings.language) }
    }
}
