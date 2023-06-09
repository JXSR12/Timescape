package edu.bluejack22_2.timescape2.custom_ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import edu.bluejack22_2.timescape2.R

internal class ButtonContainer(context: Context) : LinearLayout(context)

@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.getControlContainer(): ButtonContainer {
    this.controlContainer?.let {
        return it
    }
    val container = ButtonContainer(context)
    container.visibility = View.VISIBLE
    container.gravity = Gravity.CENTER
    val containerLayout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 200, Gravity.BOTTOM)

    this.addView(container, containerLayout)

    this.controlContainer = container
    return container
}

@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.getCameraButton(): AgoraButton {
    this.camButton?.let {
        return it
    }
    val agCamButton = AgoraButton(context = this.context)
    agCamButton.clickAction = {
        (this.context as Activity).runOnUiThread {
            it.isSelected = !it.isSelected
            it.background.setTint(if (it.isSelected) resources.getColor(R.color.colorRed) else Color.GRAY)
            this.agkit.enableLocalVideo(!it.isSelected)
        }
    }
    this.camButton = agCamButton
    agCamButton.setImageResource(R.drawable.round_videocam_off_24)
    return agCamButton
}

@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.getMicButton(): AgoraButton {
    this.micButton?.let {
        return it
    }
    val agMicButton = AgoraButton(context = this.context)
    agMicButton.clickAction = {
        it.isSelected = !it.isSelected
        it.background.setTint(if (it.isSelected) resources.getColor(R.color.colorRed) else Color.GRAY)
        this.userVideoLookup[this.userID]?.audioMuted = it.isSelected
        this.agkit.muteLocalAudioStream(it.isSelected)
    }
    this.micButton = agMicButton
    agMicButton.setImageResource(R.drawable.round_mic_off_24)
    return agMicButton
}
@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.getFlipButton(): AgoraButton {
    this.flipButton?.let {
        return it
    }
    val agFlipButton = AgoraButton(context = this.context)
    agFlipButton.clickAction = {
        this.agkit.switchCamera()
    }
    this.flipButton = agFlipButton
    agFlipButton.setImageResource(R.drawable.round_cameraswitch_24)
    return agFlipButton
}
@ExperimentalUnsignedTypes
public fun AgoraVideoViewer.getEndCallButton(): AgoraButton {
    this.endCallButton?.let {
        return it
    }
    val hangupButton = AgoraButton(this.context)
    hangupButton.clickAction = {
        this.agkit.stopPreview()
        this.leaveChannel()
    }
    hangupButton.setImageResource(R.drawable.round_close_24)
    hangupButton.background.setTint(resources.getColor(R.color.colorRed))
    this.endCallButton = hangupButton
    return hangupButton
}

@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.builtinButtons(): MutableList<AgoraButton> {
    val rtnButtons = mutableListOf<AgoraButton>()
    for (button in this.agoraSettings.enabledButtons) {
        rtnButtons += when (button) {
            AgoraSettings.BuiltinButton.MIC -> this.getMicButton()
            AgoraSettings.BuiltinButton.CAMERA -> this.getCameraButton()
            AgoraSettings.BuiltinButton.FLIP -> this.getFlipButton()
            AgoraSettings.BuiltinButton.END -> this.getEndCallButton()
        }
    }
    return rtnButtons
}

@ExperimentalUnsignedTypes
internal fun AgoraVideoViewer.addVideoButtons() {
    val container = this.getControlContainer()

    val buttons = this.builtinButtons() + this.agoraSettings.extraButtons
    container.visibility = if (buttons.isEmpty()) View.INVISIBLE else View.VISIBLE

    val buttonSize = 100
    val buttonMargin = 10f
    buttons.forEach { button ->
        val llayout = LinearLayout.LayoutParams(buttonSize, buttonSize)
        llayout.gravity = Gravity.CENTER
        container.addView(button, llayout)
    }
    val contWidth = (buttons.size.toFloat() + buttonMargin) * buttons.count()
    this.positionButtonContainer(container, contWidth, buttonMargin)
}

@ExperimentalUnsignedTypes
private fun AgoraVideoViewer.positionButtonContainer(container: ButtonContainer, contWidth: Float, buttonMargin: Float) {
    // TODO: Set container position and size

    container.setBackgroundColor(this.agoraSettings.colors.buttonBackgroundColor)
    container.background.alpha = this.agoraSettings.colors.buttonBackgroundAlpha
//    (container.subBtnContainer.layoutParams as? FrameLayout.LayoutParams)!!.width = contWidth.toInt()
    (this.backgroundVideoHolder.layoutParams as? ViewGroup.MarginLayoutParams)
        ?.bottomMargin = if (container.visibility == View.VISIBLE) container.measuredHeight else 0
//    this.addView(container)
}
