package com.example.albumnextbutton

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.example.albumnextbutton.databinding.FragmentBroadcastBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BroadcastFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class BroadcastFrag : Fragment() {

    private val sharedButtonListener: ButtonLiveData
        get() = (requireActivity().application as Helper).sharedButtonListener

    private val sharedViewModel: SharedLiveData
        get() = (requireActivity().application as Helper).sharedViewModel

    private var _binding: FragmentBroadcastBinding? = null
    private val viewBinding get() = _binding!!

    private var count: Int = 0
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBroadcastBinding.inflate(inflater, container, false)
        return viewBinding.root
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private val dataObserver = Observer<catfact> {
        var time_text = view?.findViewById<TextView>(R.id.time_sent_id)
        var time_title =  view?.findViewById<TextView>(R.id.Time_Sent)
        viewBinding.txtServerPid.text = it.data.toString()
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formatted = current.format(formatter)
        time_text?.text = formatted.toString()


    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedButtonListener.sharedButton.postValue(false)
        val button = view?.findViewById<Button>(R.id.button)

        var time_text = view?.findViewById<TextView>(R.id.time_sent_id)
        var time_title =  view?.findViewById<TextView>(R.id.Time_Sent)
        button?.setOnClickListener {
            if (button.text == "Disconnect") {
                button.text = "Connect"
                time_title?.visibility = View.INVISIBLE
                time_text?.visibility = View.INVISIBLE

                requireActivity().stopService(Intent(context, BackgroundService::class.java))

            } else {
                time_title?.visibility = View.VISIBLE
                time_text?.visibility = View.VISIBLE
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formatted = current.format(formatter)
                time_text?.text = formatted.toString()
                button.text = "Disconnect"


                requireActivity().startForegroundService(
                    Intent(
                        context,
                        BackgroundService::class.java
                    )
                )

            }
        }
        sharedViewModel.sharedMutableData.observeForever(dataObserver)

    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().stopService(Intent(context, BackgroundService::class.java))
    }
    fun onClick(v: View?) {
        if(sharedButtonListener.sharedButton.value!!){
            sharedButtonListener.sharedButton.postValue(false)
            return
        }
        sharedButtonListener.sharedButton.postValue(true)
    }
}