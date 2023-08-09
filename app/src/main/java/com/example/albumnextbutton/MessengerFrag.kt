package com.example.albumnextbutton

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.example.albumnextbutton.databinding.FragmentMessengerBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MessengerFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class MessengerFrag : Fragment(), View.OnClickListener{

    private val serverprop: ServerLiveData
        get() = (requireActivity().application as Helper).serverprop
    private val sharedViewModel: SharedLiveData
        get() = (requireActivity().application as Helper).sharedViewModel

    private val sharedButtonListener: ButtonLiveData
        get() = (requireActivity().application as Helper).sharedButtonListener

    private var _binding: FragmentMessengerBinding? = null
    private val viewBinding get() = _binding!!

    // Observer for LiveData changes
    private val dataObserver = Observer<catfact> {

    }

    private val serverObserver = Observer<Serverprop> {
        viewBinding.txtServerPid.text = it.PID
        viewBinding.txtServerConnectionCount.text = it.CCount
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMessengerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedButtonListener.sharedButton.postValue(false)
        // Start Service
        println("hey")
        val button = view?.findViewById<Button>(R.id.button)
        button?.setOnClickListener {
            if(button.text == "Disconnect"){
                button.text = "Connect"
                requireActivity().stopService(Intent(context, MessengerService::class.java))

            }
            else{
                button.text = "Disconnect"

                requireActivity().startForegroundService(Intent(context, MessengerService::class.java))

            }

        }

        // Observe the MutableLiveData
        sharedViewModel.sharedMutableData.observeForever(dataObserver)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().stopService(Intent(context, MessengerService::class.java))
        sharedViewModel.sharedMutableData.removeObserver(dataObserver)
    }



    override fun onStart() {
        super.onStart()
        serverprop.serverData.observe(viewLifecycleOwner, serverObserver)
    }

    override fun onStop() {
        super.onStop()
        serverprop.serverData.removeObserver(serverObserver)
    }

    override fun onClick(p0: View?) {
        sharedButtonListener.sharedButton.postValue(true)

    }


}