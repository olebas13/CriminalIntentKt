package org.olebas.criminalintentkt

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

class CrimePhotoZoomFragment : DialogFragment() {

    private lateinit var photoView: ImageView
    private lateinit var photoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoFile = arguments?.getSerializable(PHOTO_FILE) as File
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_crime_photo_zoom, null)

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(view)

        photoView = view?.findViewById(R.id.crime_photo_zoom) as ImageView


        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }

        return builder.create()
    }

    companion object {
        private const val PHOTO_FILE = "photo_file"

        fun newInstance(photoFile: File): CrimePhotoZoomFragment {
            val args = Bundle().apply {
                putSerializable(PHOTO_FILE, photoFile)
            }
            return CrimePhotoZoomFragment().apply {
                arguments = args
            }
        }
    }
}