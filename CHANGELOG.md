# Change Log

## Version 0.4.0
_2020_11_06_

* Add Default values feature
* Add resetSessionId API
* Update Keen client to fix mkdir issue

## Version 0.3.0
_2019_10_14_

* Add feature auto tracking Advertising Id (`TreasureData#enableAutoAppendAdvertisingIdentifier()`)

## Version 0.2.0
_2019-06-13_

* Add support for ProfileAPI (`TreasureData#fetchUserSegments()`)

## Version 0.1.19
_2018-12-20_

* Add auto in-app purchase event tracking

## Version 0.1.18
_2018-08-03_

* Auto event tracking is optional and off by default
* Add functions for auto tracking events and custom event opt out
* Add Opt Out example for td-android-sdk-demo

## Version 0.1.17
_2018-03-01_

* Add Auto tracking events
* Update TreasureData#sharedInstance to be singleton

## Version 0.1.16
_2017-03-13_

* Add TreasureData#getSessionId

## Version 0.1.15
_2017-02-17_

* Add TreasureData#setMaxUploadEventsAtOnce(int maxUploadEventsAtOnce)
* Upload at most limited number (default: 400) of events at once to prevent OOM

## Version 0.1.14
_2016-09-30_

* Add TreasureData#enableAutoAppendRecordUUID()
* Add TreasureData#enableServerSideUploadTimestamp(String columnName)

## Version 0.1.13
_2016-06-23_

* Add TreasureData.getSessionId(Context)
* Fix the bug that the second call of TreasureData.startSession(Context) unexpectedly updates the session ID without calling TreasureData.endSession(Context)

## Version 0.1.12
_2016-04-08_

* Add TreasureData.setSessionTimeoutMilli()

## Version 0.1.11
_2016-02-10_

* Add a pair of class methods TreasureData.startSession() and TreasureData.endSession() that manages a global session tracking over Contexts. Even after TreasureData.endSession() is called and the activity is destroyed, it'll continue the same session when TreasureData.startSession() is called again within 10 seconds
* Append application package version information to each event if TreasureData#enableAutoAppendAppInformation() is called
* Append locale configuration information to each event if TreasureData#enableAutoAppendLocaleInformation() is called

## Version 0.1.10
_2016-02-05_

* Fix the bug that can cause a failure of sending HTTP request

## Version 0.1.9
_2016-01-07_

* Enable server side upload timestamp

## Version 0.1.8

* Remove confusable and useless APIs
* Improve the retry interval of HTTP request
* Reduce the number of methods in sharded jar file and the library file size

## Version 0.1.7 (skipped)

## Version 0.1.6

* Append device model infromation and persistent UUID which is generated at the first launch to each event if it's turned on
* Add session id
* Add first run flag so that the application detects the first launch
* Retry uploading
* Remove gd_bundle.crt from Java source file

## Version 0.1.5

* Fix some minor bugs

## Version 0.1.4

* Fix some bugs related to encryption

## Version 0.1.3

* Improve error handling with TreasureData#addEventWithCallback() and TreasureData#uploadEventsWithCallback()
* Enable the encryption of bufferred event data with TreasureData.initializeEncryptionKey()

## Version 0.1.2

* Implement gd_bundle.crt into Java source file

## Version 0.1.1

* Add shaded jar file

