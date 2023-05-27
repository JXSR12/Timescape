package edu.bluejack22_2.timescape.custom_ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.amulyakhare.textdrawable.TextDrawable
import edu.bluejack22_2.timescape.R
import io.agora.rtc2.video.VideoCanvas

/**
 * View for the individual Agora Camera Feed.
 */
@ExperimentalUnsignedTypes
class AgoraSingleVideoView(context: Context, uid: Int, micColor: Int, private val userId: String, private val displayName: String) : FrameLayout(context) {
    // New TextView for displaying the displayName
    lateinit var displayNameView: TextView
    private var retryCount = 0
    private val maxRetries = 10
    private val retryDelay = 1000L // 1 sec

    // Get projectId from context's intent
    val projectId = (context as Activity).intent.getStringExtra("projectId") ?: ""
    /**
     * Canvas used to render the Agora RTC Video.
     */
    lateinit var canvas: VideoCanvas
        internal set
    internal var uid: Int = uid
//    internal var textureView: AgoraTextureView = AgoraTextureView(context)

    /**
     * Is the microphone muted for this user.
     */
    var audioMuted: Boolean = true
        set(value: Boolean) {
            field = value
            (context as Activity).runOnUiThread {
                this.mutedFlag.visibility = if (value) VISIBLE else INVISIBLE
            }
        }

    /**
     * Is the video turned off for this user.
     */
    var videoMuted: Boolean = true
        set(value: Boolean) {
            if (this.videoMuted != value) {
                this.backgroundView.visibility = if (!value) INVISIBLE else VISIBLE
//                this.textureView.visibility = if (value) INVISIBLE else VISIBLE
            }
            field = value
        }

    internal val hostingView: View
        get() {
            return this.canvas.view
        }

    /**
     * Icon to show if this user is muting their microphone
     */
    var mutedFlag: ImageView
    var backgroundView: FrameLayout
    var micFlagColor: Int = micColor

    /**
     * Create a new AgoraSingleVideoView to be displayed in your app
     * @param uid: User ID of the `AgoraRtcVideoCanvas` inside this view
     * @param micColor: Color to be applied when the local or remote user mutes their microphone
     */
    init {
        setupSurfaceView()
        this.backgroundView = FrameLayout(context)
        this.setBackground()
        this.mutedFlag = ImageView(context)
        this.setupMutedFlag()

        displayNameView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.START
            ).apply {
                leftMargin = DPToPx(context, 5)
                bottomMargin = DPToPx(context, 5)
            }
            textSize = 12f
            setTextColor(Color.WHITE) // change color as needed
            text = displayName
        }

        addView(displayNameView)

        this.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun isActivityRunning(): Boolean {
        return if (context is Activity) {
            !(context as Activity).isFinishing
        } else {
            false
        }
    }

    private fun setupSurfaceView() {
        val surfaceView = SurfaceView(context)
        this.canvas = VideoCanvas(surfaceView)
        this.canvas.uid = uid

        addView(surfaceView, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT))

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                retryCount = 0
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (holder.surface.isValid) {
                    retryCount = 0
                } else if (retryCount < maxRetries) {
                    retryCount++
                    postDelayed({ setupSurfaceView() }, retryDelay)
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Surface is destroyed, nothing to do here for now
            }
        })
    }

    private fun setupMutedFlag() {

        val mutedLayout = FrameLayout.LayoutParams(DPToPx(context, 25), DPToPx(context, 25))
//        mutedLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        mutedLayout.gravity = Gravity.BOTTOM or Gravity.END
//        mutedLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        mutedLayout.bottomMargin = DPToPx(context, 5)
        mutedLayout.leftMargin = DPToPx(context, 5)

        mutedFlag.setImageResource(R.drawable.round_mic_off_24)

        mutedFlag.setColorFilter(this.micFlagColor)
        addView(mutedFlag, mutedLayout)
        this.audioMuted = true
    }

    fun setBackground() {
        backgroundView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        )
        backgroundView.setBackgroundColor(Color.DKGRAY)
        addView(backgroundView)

        val avatarDrawable = generateAvatar(userId)
        val personIcon = ImageView(context)
        personIcon.setImageDrawable(avatarDrawable)
        val buttonLayout = FrameLayout.LayoutParams(150, 150)
        buttonLayout.gravity = Gravity.CENTER
        backgroundView.addView(personIcon, buttonLayout)
    }

    private fun generateAvatar(userId: String): TextDrawable {
        val content = "\\O/"
        val random = kotlin.random.Random(userId.hashCode())
        val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
        return TextDrawable.builder().buildRound(content, color)
    }
}
