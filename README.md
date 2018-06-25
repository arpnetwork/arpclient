
# ARP Client
ARP Client is a tool for Android devices to use the app rendering power of other smart devices.

Get remote device screen. (Achieved in Version 1.0)
Interact with remote devices. (Will be handle in followed-up version)

## Usage

### Start
Download the module and copy it into the root direction of your project
Include the module in settings.gradle
```java
include ':arpclient'
```
### Init SDK
Init SDK in ArpApplication.java
```java
public class ArpApplication extends Application {  
    @Override  
    public void onCreate() {  
        super.onCreate();  
  
        ArpClient.init(this);
    }  
  
    @Override  
    public void onTerminate() {  
        super.onTerminate();  
  
       ArpClient.fini();
    }  
}
```
Set permission and application config in AndroidMenifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />

<application  
    android:name=".ArpApplication"  
    ...
>  
	...
</application>
```

### Get remote device screen
Use MediaPlayer to get video of remote device screen
```java
private MediaPlayer mMediaPlayer;

private void init() {
    // init media player
    mMediaPlayer = new MediaPlayer();
    mMediaPlayer.setOnMediaPlayerListener(this);
}

public void setDataSource(Uri uri) {
    // set data source and play
    mMediaPlayer.setDataSource(uri);
    mMediaPlayer.prepareAsync();
}

@Override
public void onPrepared(MediaPlayer player) {
    // do something when video begin render
    // for example,
    // for it may take a few seconds to connect to a appropriate device,
    // you can set up a progress indicator when create the activity
    // and dismiss it here
}

@Override
public void onClosed() {
    // do something when connection closed
}

@Override
public void onError(int errorCode, String msg) {
    // do something when errors happened
}
```
Set surface for MediaPlayer in SurfaceTextureListener
```java
@Override
public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
    mMediaPlayer.setSurface(new Surface(surfaceTexture));
    mMediaPlayer.reconnect();
}

@Override
public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    mMediaPlayer.disconnect();
    if (surfaceTexture != null) {
        surfaceTexture.release();
    }
    return true;
}
```
