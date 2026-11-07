package com.digitalvault

import android.app.Application
import com.digitalvault.data.VaultDatabase
import com.digitalvault.data.VaultRepository
import com.digitalvault.reminder.ExpiryScheduler
import com.digitalvault.security.KeyVault
import com.digitalvault.settings.AppSettings

class DigitalVaultApp : Application() {
    lateinit var vault: KeyVault
        private set
    lateinit var repository: VaultRepository
        private set
    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        vault = KeyVault(this)
        repository = VaultRepository(vault, VaultDatabase.get(this))
        settings = AppSettings(this)
        ExpiryScheduler.schedule(this)
    }
}
