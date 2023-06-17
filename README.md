# Timescape
## Project Management and Messenger Application
----------------------------------------------
Timescape is a project management and messaging application designed to streamline team collaboration and project tracking.

- Create and manage projects easily
    * Easily store, manage, and track your project progress
- Collaborate in real time
    * See what your project members are doing, view their online status, and interact with them in real time
- Built in robust communication system
    * Comes with a robust messenger providing a hassle-free yet feature-rich chatting with your project mates
- Invite new members quick and easy
    * Know someone using the app? Search their name or phone number and invite them.
- One-click invite system
    * Don't know who to add? You can just generate an invite link and post it wherever you want! People are gonna be one tap away from joining your project.

## Changelog

### 0.6.3: Message Reply Bugfixes
- Fixed app crashing when replying to unsent message with more than 1 mentions.
- Fixed replied message not showing in the reply message when replying to a project invite message.

### 0.6.2: Project Live Rooms - Video Call and Screen Sharing
- Released the new project live room feature, instantly join the project live room via the button on the top right corner in a project's chat room.
- User can turn off/on video, turn off/on microphone, and switch their camera.
- User can choose one other user to pin their video.
- User can share their screen to other members in the room.
- Video and voice chatting is implemented using Agora Android SDK.
- Revamped a lot of UI in different sections of the app.
- Changed a few icons to be more representing.
- Users can no longer change their display name to empty string.
- The live room UI is a bit different from the VC Testing experimental version, we decided to just adapt from the Agora Video UI Kit by directly modifying the Kotlin classes and use those instead of the original.

- Plans for 0.6.X going forward:
  * Tasks will be more interactive, each task will have their own progress, and can be assigned/unassigned to specific members.
  * Members will be able to filter tasks to only show those assigned to them.
  * Project Space will be making its debut, it is sort of a hub for projects, where members can share post and files so it can be easily discovered. This will be separated from the files/media shared in the Project Chat.
  * We have been focusing a lot on the 'Real Time Messenger' side of the app, and have just began touching the 'Video Conferencing' side. Now, we will be shifting our focus a bit to develop more features for the 'Project Management' side of the app for 0.6.X.

- NOTE: From this version onwards, the APK files will be split into different ABIs. The in app auto updater should automatically choose the right version based on the device preferred ABI.
- NOTE: The universal APK is still available in case the preferred ABI is not found, but it is at 240 MB in size. (The ABI specific APKs are around 60-70 MB in size)

### 0.5.8: HOTFIX App crash on sending message
- Supposedly fixed where sometimes on poor connection sending message would cause the app to crash.
- We are working to investigate this problem further. However, in this patch we have not yet experienced the same issue this far.

### 0.5.7: Lists UX Improvement Update
- Added touch effect on project chats list in 'Project Chats' page
- Added placeholders for various list displays if empty.
- Translated the project operation notification and inbox message.
- Changed so that the floating button to Scan QR and Create Project only appears when the user is in Dashboard, providing a more intuitive UX.

### 0.5.6: No network warning dialog
- After observing some oddities in behavior of the app and crashes caused by the absence of internet connection, we have added a warning dialog that will show up everytime user launches an activity without a proper internet connection. This dialog is dismissable by clicking on the 'I understand. Continue anyway' button.

### 0.5.5c: More translation fixes
- Fixed and added more translations
- Proofreaded most parts for Indonesian translation

### 0.5.5: Mentions Enhancement Update, View Reads, Read More layout
- Fixed mentions not being saved after the last chat UX update
- Set the mention string display in chat messages to be the same as in the message input, making UX more consistent
- Added the feature to display list of users who have read a message (only for self message)
- Added the 'Read more' feature on long chat messages to prevent it from taking too much space and disturbing navigation through messages.

### 0.5.3: Project and Tasks Filters
- Added filtering options for All Projects and All Tasks in Dashboard
- Added some more translations

### 0.5.2: Server timestamp sync update
- Every chat message and some other timestamp based actions are now configured to always use the server's current time instead the device time.

### 0.5.1: Video Player Fix
- Fixed video player not hiding controls in a way that is intuitive for the user playing a video.
- Fixed video player not stopping when the image viewing activity is paused or destroyed.

### 0.5.0: Chat Muting + Image Navigation
- This was going to be called 0.3.11 but after some consideration, I think the 2 features added are significant enough to switch into a new minor version. And yes, 0.4 is skipped due to some unexplainable reasons :)
- Enabled swiping left/right to navigate between the images/videos attachment inside a project chat
- Fully implemented notifications muting for project chats, mute can be done via the chat room, notification and project chats page. Unmute can be done via the chat room and project chats page

### 0.3.10b: Translations Added
- Marked this version to be the end of P3 (Phase 3) of the development of Timescape
- Added translation for updater download dialog

### 0.3.10: Image Free Cropping + Video Marker Update
- Enabled free shape cropping in image editing before sending image attachment.
- Added an overlay marking an image attachment as a video if it is a video. (This is implemented only for videos uploaded after version 0.3.8)

### 0.3.9: Unread messages indicator fix + Installer fix
- Fixed the new unread message indicator when scrolling up a chat room not behaving correctly.
- Fixed the updater not installing the correct file after downloading latest APK.
- Added a progress bar display when updating app.

### 0.3.8: Chat UX improvement + Video attachment
- Added an option to choose video as attachment when clicking on the image attach button
- Changed the way send button in chat input is displayed to give better UX
- Chats no longer scroll to bottom automatically on new message arrival when the user is not at the bottom.

### 0.3.7: Image Editing Fixes
- Fixed image cropping to use source image's aspect ratio
- User can no longer upload video as image attachment, please use the file attachment instead. A separate video attachment feature will be added in the future
- Added image placeholders for image attachments
- Few more fixes

### 0.3.6: Image Enhancement Update + Notif Reply Rework
- Added a feature to zoom image attachments
- Added a feature to crop and scale images before sending as attachment (Thanks to UCrop library)
- Reworked the whole notification reply feature to fix inconsistencies.

### 0.3.5b: Reply from Notif Fix
- Fixed where replying chat from message notification is not working after the chat revamp update.
- Fixed where project owners do not receive message push notification if the message is sent from notification reply button.

### 0.3.5: Manual Update Check and Translations update
- Displayed the current application version in profile page.
- Added a check for updates button to manually trigger update checking
- Added more translations especially in profile editing feature.
- Skipped version 0.3.4
- NOTE: Through the release of this version, all the core features planned in the beginning of development have already been implemented, should expect less frequent major updates and finally reach some stability.

### 0.3.3b: Updater Hotfix
- Fixed wrong version checking method for updater

### 0.3.3: Launcher Icon and Auto Updater update
- Added the application icon (finally)
- Added an auto-updater system to use while the app is not on play store yet

### 0.3.0: Profile Update and File Open update
- Added the profile page to view and edit profile
- User can now send a project invite to any project chat
- Fixed dark/light theme to function properly
- Fixed video player keeps playing in the background after closing the activity
- File attachment now detects if the user already downloaded the file and is able to open the file from inside the app
- Added progress dialog when uploading file/image
- Implemented a thumbnail system for images to reduce initial load
- A lot of quality of life changes
- More translations

### 0.2.4: Project Essentials Fortifications
- Changed the way recently accessed projects are stored, giving a more consistent experience.
- Fixed the issue where some users still have a deleted/left/removed project on their recently accessed tiles.
- Added extra layers of verification to make sure the user is still a member of the project before doing any mutative action on the project via unrefreshed UI elements.

### 0.2.3: Permissions, Read System and Status update
- Improved the way the app request for permissions
- Fixed not being able to use the QR Scanner after initially granting camera permission
- Overhauled online/typing status to be more responsive to changes and more efficient.
- Overhauled the chat read system to be more efficient.
- Fixed some messages not being marked as read on activity resume if the reader is pausing the chat activity when the message is sent.

### 0.2.0: Project Chats Speed and Efficiency Update
- Completely overhauled how chats store and retrieve messages to be significantly faster and more efficient.
- Changed the displayed message for notifications and chat previews for image and file attachments.
- Improved the way chat messages retrieve the message details to be faster and more efficient.
- Added more translations.
- NOTE: Users should now feel little to no flickering in user display names when chatting in a more intense frequency.
- NOTE: The new way of storing chat messages also fixes the possibility of a collision (which previously results in some messages being lost) when multiple user sends message to the same chat room at the same time.

### 0.1.9: Notifications Rework and QR Invite System
- Added QR Code when generating invite link for projects.
- Added a QR Code scanner to directly join project from QR codes.
- Completely revamped notification system to use Cloud Messaging.
- Added more translations.
- A few quality of life changes.
- Fixed inconsistencies with project deadline notifications.
- Other smaller bugfixes.

### 0.1.x: Initial release
- Initial release of the app
- All changes before this was not logged for versioning

