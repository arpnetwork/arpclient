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
    // set video quality before start
    // default to LOW
    ARPClient.setQuality(Quality.HIGH);

    // init ARPClient with context and listener
    mARPClient = new ARPClient(getContext(), this);
    // set TextureView for ARPClient to render video and catch touch events
    // The TextureView must be set for ARPClient when init and before started.
    // The TextureView must be placed in FrameLayout and match screen.
    mARPClient.setSurfaceView(textureView);
    // start connection with ip and port of remote device, session for authentication
    // and package name of launching app
    // All parameter must not be null.
    mARPClient.start(host, ip, session, packageName);
}
```

ARPClient listener.
```java
@Override
public void onPrepared() {
    // do something when is ready for play
    // for example,
    // for it may take a few seconds to connect to the remote device,
    // a progress indicator can be set up when creating the activity
    // and dismissed here
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
