# td-logger-android

td-mobile-sdk is a library to send any data to Treasure Data directly from Android without td-agent(fluentd). td-mobile-sdk is so small that it's easy to use for Android development.

## Requirement

Android OS >= 2.2

## Installation

You can install td-mobile-sdk into your Android project in the following ways.

### Maven

If you're using maven, please add the following directive into your pom.xml

    now implimenting...

### Jars
Also, you can use td-mobile-sdk when you put td-mobile-sdk.jar into (YOUR_ANDROID_PROJECT)/libs.

## Usage

### How to pass API key to TdAndroidLogger

td-mobile-sdk use Treasure Data API, so this library needs the API key of users. You can pass your API key to td-mobile-sdk in the following ways.

#### Java constractor

    final String apiKey = "1Qaz2WSx3eDc4RfvBGt56yHnMjU78ik";
    new TdAndroidLogger(apiKey);

#### res/values/td-logger.xml

Also, if you put res/values/td-logger.xml in the following format, you can use td-mobile-sdk without writting APK key in Java code.

    now implimenting...

### Increment counter

If you want to only increment counters, you might as well use increment() APIs. They aggregate the values and use less memory and network.

    now implimenting...

### Send record

### About flushing timing

### Cleaning up

