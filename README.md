# visync
Quick prototype of an app that allows you to use multiple android devices as a one big screen when playing a video. 

![visync-demonstration-picture](https://github.com/Ampiduxmoe/visync/assets/63016382/b9059c09-cc52-4f27-b40c-5b1746d0a3c2)


It uses [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) for video playback and [Google Nearby Connections](https://developers.google.com/nearby/overview) for connectivity/synchronization.

After host device has configured what part of the video each connected device should display, app uses user-provided information on devices dimensions and display sizes to correctly calculate video frame sizes and positions.  

![visync-editor-on-tablet](https://github.com/Ampiduxmoe/visync/assets/63016382/6f8077a8-02d2-4e86-888f-bf441ca59826)

# Usage overview
### As a host
1. Open the app.
2. Open side drawer (if you specified your device dimensions already you can skip to step 4).
3. Enter your display size in inches, width and height in pixels and device body dimensions in millimeters. 
4. Select a file to play by tapping on a button with a plus sign inside.
5. Specify playback settings using "Playback settings" tab.
6. Go to "Connections" tab and tap on "Toggle room visibility" button on the bottom. Then wait for others to connect and allow them to participate in group playback by tapping on screen icon next to them or by tapping on row that corresponds to their user and then tapping "Allow participation" when "User information" dialog pops up.
7. Tap on "View configuration" and set up device positions, video position and video size either using gestures or Settings button on the bottom right.
8. Tap "Play!" button.

![visync-host-screenshots](https://github.com/Ampiduxmoe/visync/assets/63016382/aa585151-e301-426d-a3bc-24f90841cae9)


### As a guest
1. Open the app.
2. Open side drawer (if you specified your device dimensions already you can skip to step 4).
3. Enter your display size in inches, width and height in pixels and device body dimensions in millimeters. 
4. Go to "Join" screen by tapping on respective destination on the bottom or on the side drawer.
5. Tap on the magnifying glass icon and select a row that contains username of the host you want to connect to (you can check your username on the side drawer).
6. After you successfully connect you should see list of videos that host selected for group playback.
7. Tap on the video that your group wants to play and choose "Select file" on a dialog that pops up, then select file on your device that matches that video.
8. Wait until host starts playback.

![visync-guest-screenshots](https://github.com/Ampiduxmoe/visync/assets/63016382/6b88063e-3bc1-45a2-9afd-e82833053687)
