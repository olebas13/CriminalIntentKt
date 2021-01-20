package org.olebas.criminalintentkt

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import java.util.*

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callSuspectButton: Button

    private var phoneAccess = false

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        timeButton = view.findViewById(R.id.crime_time)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        reportButton = view.findViewById(R.id.crime_report)
        suspectButton = view.findViewById(R.id.crime_suspect)
        callSuspectButton = view.findViewById(R.id.call_suspect)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner, Observer { crime ->
            crime?.let {
                this.crime = crime
                updateUI()
            }
        })

        if (ContextCompat.checkSelfPermission(activity!!.applicationContext,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_CONTACTS),
                MY_PERMISSIONS_REQUEST_READ_CONTACTS)
        } else {
            phoneAccess = true
            callSuspectButton.isEnabled = true
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                // empty
            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // empty
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, R.string.crime_report_subject)
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolverActivity: ResolveInfo? = packageManager.resolveActivity(pickContactIntent,
                PackageManager.MATCH_DEFAULT_ONLY)

            if (resolverActivity == null) {
                isEnabled = false
            }
        }

        callSuspectButton.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${crime.phone}"))
            startActivity(callIntent)
        }

        if (!phoneAccess) {
            callSuspectButton.isEnabled = false
        }

    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data

                val queryFields = arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )

                val contentResolver = requireActivity().contentResolver
                val contacts = contentResolver.query(contactUri!!, queryFields,
                    null, null, null)
                contacts?.use {
                    if (it.count == 0) {
                        return
                    }

                    it.moveToFirst()
                    val suspect = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                    val hasNumber = it.getString(it.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt()

                    if (hasNumber >= 1 && phoneAccess) {
                        val numbers = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                            null,
                            null
                        )

                        Log.d(TAG, "$numbers $id $hasNumber")

                        numbers?.use { number ->
                            number.moveToFirst()
                            val phone = number.getString(number.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            crime.phone = phone
                        }
                    } else {
                        crime.phone = ""
                    }

                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    phoneAccess = true
                }
                return
            }
        }
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(hour: Int, minutes: Int) {
        crime.date.hours = hour
        crime.date.minutes = minutes
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = DateFormat.format(DATE_FORMAT, crime.date)
        timeButton.text = DateFormat.format(TIME_FORMAT, crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }

        callSuspectButton.isEnabled = crime.phone.isNotEmpty()
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }


    companion object {
        private const val ARG_CRIME_ID = "crime_id"
        private const val TAG = "CrimeFragment"
        private const val DIALOG_DATE = "DialogDate"
        private const val DIALOG_TIME = "DialogTime"
        private const val REQUEST_DATE = 0
        private const val REQUEST_TIME = 1
        private const val REQUEST_CONTACT = 2
        private const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 4
        private const val DATE_FORMAT = "EEE, d MMM yyyy"
        private const val TIME_FORMAT = "HH:mm"

        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }

            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}