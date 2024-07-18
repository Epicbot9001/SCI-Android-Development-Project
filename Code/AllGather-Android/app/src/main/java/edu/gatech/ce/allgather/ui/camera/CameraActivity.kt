package edu.gatech.ce.allgather.ui.camera

import android.os.Bundle
import android.view.View
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.base.BaseActivity
import edu.gatech.ce.allgather.databinding.ActivityCameraBinding

class CameraActivity : BaseActivity() {
    companion object {
        /** Combination of all flags required to put activity into immersive mode */
        const val FLAGS_FULLSCREEN = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        val binding = ActivityCameraBinding.inflate(layoutInflater)
        binding.rootLayout.postDelayed({
            binding.rootLayout.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }
}