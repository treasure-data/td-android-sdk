# td-android-sdk

td-android-sdk is a library to send any data to Treasure Data storage directly from Android applications without td-agent(fluentd). td-android-sdk is so small that it's easy to use for Android application development.

## Requirement

Android OS >= 2.2

## Installation

You can install td-android-sdk into your Android project in the following ways.

### Maven

If you're using maven, add the following directives to your pom.xml

    <repositories>
	  <repository>
	    <id>treasure-data.com</id>
	    <name>Treasure Data's Maven Repository</name>
	    <url>http://maven.treasure-data.com/</url>
	  </repository>
    </repositories>

    <dependencies>
      <dependency>
        <groupId>com.treasure_data</groupId>
        <artifactId>td-android-sdk</artifactId>
        <version>0.0.1-SNAPSHOT</version>
      </dependency>
    </dependencies>

### Jar file

Or put td-android-sdk.jar into (YOUR_ANDROID_PROJECT)/libs.

## Settings

### AndroidManifest.xml

Ensure that your app requests INTERNET permission by adding this line to your AndroidManifest.xml.

    <uses-permission android:name="android.permission.INTERNET" />

In addition to it, add the following lines to allow to run TdLoggerService

    <application>
        <service android:name="com.treasure_data.androidsdk.logger.TdLoggerService"></service>
    </application>

#### res/values/td-logger.xml

Write your API key in res/values/td.xml.

    <resources>
        <string name="td_apikey">1Qaz2WSx3eDc4RfvBGt56yHnMjU78ik</string>
    </resources>

## Usage

### Instantiate TdLogger

Usually, DefaultTdLogger is best because it's non-blocking and robust.

    TdLogger logger = new DefaultTdLogger();

### Increment counter

If you want to only increment some counters, you might as well use TdLogger.increment() APIs. They aggregate the values and use less memory and network.

    viewNaviSignUp.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            logger.increment("foo_db", "bar_tbl", "navi_signup");
                :

### Send record

Of course, you can send various information other than counter.

    viewLargeImage.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            logger.write("foo_db", "bar_tbl", "large_image_touch", ev.toString());
                :

### Flushing buffered data

TdLogger.write() and increment() only buffer the data without sending it to Treasure Data storage. The data will be sent automatically at regular intervals.

### Cleaning up

When you finish using TdLogger, you need to call TdLogger.close() in order to release its resources.

    logger.close()

## Example

This project includes an Android example project. Let's run example/td-android-sdk-demo as Android project.
