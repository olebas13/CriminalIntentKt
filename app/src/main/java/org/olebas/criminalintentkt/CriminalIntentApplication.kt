package org.olebas.criminalintentkt

import android.app.Application
import org.olebas.criminalintentkt.database.CrimeRepository

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }

}