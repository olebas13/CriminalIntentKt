package org.olebas.criminalintentkt

import androidx.lifecycle.ViewModel
import org.olebas.criminalintentkt.database.CrimeRepository

class CrimeListViewModel : ViewModel() {

    private val crimeRepository = CrimeRepository.get()

    val crimeListLiveData = crimeRepository.getCrimes()

}