package org.olebas.criminalintentkt

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.sql.Time
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class TimePickerFragment : DialogFragment() {

    interface Callbacks {
        fun onTimeSelected(hour: Int, minutes: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val timeListener = TimePickerDialog.OnTimeSetListener {
                _: TimePicker, hourOfDay: Int, minute: Int ->


            targetFragment?.let { fragment ->
                (fragment as Callbacks).onTimeSelected(hourOfDay, minute)
            }
        }

        val date = arguments?.getSerializable(ARG_TIME) as Date
        val initialHour = date?.hours
        val initialMinute = date?.minutes


        return TimePickerDialog(
                requireContext(),
                timeListener,
                initialHour,
                initialMinute,
                true
        )
    }

    companion object {
        private const val ARG_TIME = "time"

        fun newInstance(date: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, date)
            }

            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }
}