# ARP Client

ARP Client is a tool for Android devices to use the app rendering power of other smart devices.

## Usage

### Start
Download the module and copy it into the root direction of your project.
Include the module in settings.gradle.
```java
include ':arpclient'
```
Add implementation project in build.gradle.
```java
dependencies {
    implementation project(':arpclient')
}
```

### Init SDK
Init SDK in ARPApplication.java.
```java
public class ARPApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ARPClient.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

       ARPClient.fini();
    }
}
```
Set permission and application config in AndroidMenifest.xml.
```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:name=".ARPApplication"
    ...
>
    ...
</application>
```

### Get remote device

Init APRClient with ARPClientListener.
Set TextureView and device Uri.
```java
private ARPClient mARPClient;

private void init() {
    // init ARPClient with listener
    mARPClient = new ARPClient(this);
    // set TextureView for ARPClient to render video and catch touch events
    mARPClient.setSurfaceView(surfaceView);
    // set remote device uri for connection
    mARPClient.start(remoteDeviceUri);
}
```

ARPClient listener.
```java
@Override
public void onPrepared() {
    // do something when is ready for play
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
    // do something when errors occurred
}
```

Stop connection.
```java
mARPClient.stop();
```
