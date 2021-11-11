# WiFiMonitor
[![](https://jitpack.io/v/Kuama-IT/android-kotlin-wifi-watcher.svg)](https://jitpack.io/#Kuama-IT/android-kotlin-wifi-watcher)

Allows you to watch for wi-fi changes on an Android device.

```kotlin
val wifiMonitor = WifiMonitor.Builder()
    .listener(wifiListener)
    .wifiManager(wifiManager)
    .permissionChecker(permissionChecker)
    .build(context)

val callbackFlow = wifiMonitor.monitor()

// use the flow as you like

```

### Do I need a context?
Sadly yes, to bootstrap the library you will need a valid context.

### Installation
Add the JitPack repository to your build file
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependency

```
dependencies {
    implementation 'com.github.Kuama-IT:android-kotlin-wifi-watcher:Tag'
}
```
### Which states could I get?

#### `CONNECTED`
**only if you obtained the `ACCESS_FINE_LOCATION` permission**
<br>
When the wi-fi is connected.
<br>
With this state you also get the ssid of the wifi.
 

#### `CONNECTED_MISSING_FINE_LOCATION_PERMISSION`
When the wi-fi is connected and your user didn't grant the `ACCESS_FINE_LOCATION` permission.

#### `DISCONNECTED`
When the phone is not connected to any Wi-Fi.

#### `UNKNOWN`
When the library could not recognize the Wi-Fi state.
