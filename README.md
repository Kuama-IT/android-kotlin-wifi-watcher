# Wi-Fi Spy
Allows you to watch for wi-fi changes on an Android device.

```kotlin
val info = WiFiSpy.with(this).info

subscribe = info.subscribe {
    println("${it.state.name} - ${it.ssid}")
}

```
don't remember to unsubscribe when you don't need it anymore
```kotlin
override fun onPause() {
    super.onPause()
    if (subscribe?.isDisposed == false) {
        subscribe?.dispose()
    }
}
```

### Do I need a context?
Sadly yes, to bootstrap the library you will need a valid context:

```kotlin
WiFiSpy.with(this) // this is a context (eg. activity)
```

### Can I subscribe multiple times?
Yes, the `info` object is a `ConnectableObservable`

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