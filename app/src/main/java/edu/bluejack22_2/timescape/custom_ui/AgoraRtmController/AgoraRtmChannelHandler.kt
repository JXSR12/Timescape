package edu.bluejack22_2.timescape.custom_ui.AgoraRtmController

import edu.bluejack22_2.timescape.R
import edu.bluejack22_2.timescape.custom_ui.AgoraVideoViewer
import io.agora.rtm.RtmChannelAttribute
import io.agora.rtm.RtmChannelListener
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Class for all the Agora RTM Channel event handlers
 *
 * @param hostView [AgoraVideoViewer]
 */
@ExperimentalUnsignedTypes
open class AgoraRtmChannelHandler(private val hostView: AgoraVideoViewer) : RtmChannelListener {

    val TAG = this.hostView.resources.getString(R.string.TAG)

    override fun onMemberCountUpdated(memberCount: Int) {
        Logger.getLogger(TAG).log(Level.INFO, "RTM member count updated : $memberCount")
        this.hostView.rtmChannelOverrideHandler?.onMemberCountUpdated(memberCount)
    }
    override fun onAttributesUpdated(attributeList: MutableList<RtmChannelAttribute>?) {
        Logger.getLogger(TAG).log(Level.INFO, "RTM Channel attributes updated")
        this.hostView.rtmChannelOverrideHandler?.onAttributesUpdated(attributeList)
    }
    override fun onMessageReceived(
        rtmMessage: RtmMessage,
        rtmChannelMember: RtmChannelMember
    ) {
        Logger.getLogger(TAG).log(Level.INFO, "RTM Channel Message Received")
        AgoraRtmController.messageReceived(rtmMessage.text, hostView)
        this.hostView.rtmChannelOverrideHandler?.onMessageReceived(rtmMessage, rtmChannelMember)
    }

    override fun onMemberJoined(rtmChannelMember: RtmChannelMember) {
        Logger.getLogger(TAG).log(Level.SEVERE, "RTM member : ${rtmChannelMember.userId}  joined channel : ${rtmChannelMember.channelId}")
        AgoraRtmController.sendUserData(toChannel = false, peerRtmId = rtmChannelMember.userId, hostView = this.hostView)

        this.hostView.rtmChannelOverrideHandler?.onMemberJoined(rtmChannelMember)
    }
    override fun onMemberLeft(rtmChannelMember: RtmChannelMember) {
        Logger.getLogger(TAG).log(Level.SEVERE, "RTM member left ${rtmChannelMember.userId} from channel ${rtmChannelMember.channelId}")

        this.hostView.rtmChannelOverrideHandler?.onMemberLeft(rtmChannelMember)
    }
}
