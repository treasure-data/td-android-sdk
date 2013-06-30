# td-mobile-sdk

td-mobile-sdk is a library to send any data to Treasure Data directly from Android without td-agent(fluentd). td-mobile-sdk is so small that it's easy to use for Android development.

## Requirement

Android OS >= 2.2

## Installation

You can install td-mobile-sdk into your Android project in the following ways.

### Maven

If you're using maven, please add the following directive into your pom.xml

    <dependencies>
           :
        <dependency>
            <groupId>com.treasure_data</groupId>
            <artifactId>td-mobile-sdk</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
           :
    </dependencies>

### Jars
Also, you can use td-mobile-sdk when you put td-mobile-sdk.jar into (YOUR_ANDROID_PROJECT)/libs.

## Usage

### How to pass API key to TdAndroidLogger

td-mobile-sdk uses Treasure Data API, so this library needs the API key of the user. You can pass your API key to td-mobile-sdk in the following ways.

#### Java constractor

    final String apiKey = "1Qaz2WSx3eDc4RfvBGt56yHnMjU78ik";
    TdAndroidLogger logger = new TdAndroidLogger(apiKey);

#### res/values/td-logger.xml

Also, you put res/values/td.xml in the following format and you can use td-mobile-sdk without writting APK key in Java code.

    <resources>
        <string name="td_apikey">1Qaz2WSx3eDc4RfvBGt56yHnMjU78ik</string>
    </resources>

### Increment counter

If you want to only increment some counters, you might as well use increment() APIs. They aggregate the values and use less memory and network.

    viewNaviSighUp.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            logger.increment("foo_db", "bar_tbl", "navi_signup");
                :

### Send record

Of couse, you can send various information not only counter.

    viewLargeImage.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            logger.write("foo_db", "bar_tbl", "large_image_touch", ev.toString());
                :

## Flushing buffered data

write() and increment() only buffer the data without sending immediately. So you need to flush the data manually or automatically.

### Flush manually

You can flush the specified buffered data using flush().

    logger.flush("foo_db", "bar_tbl");

Also, you can flush all the buffered data using flushAll().

    logger.flushAll();

### Flush automatically

If you call startAutoFlushing(), buffered data will be flushed automatically at regular intervals.

    logger.startAutoFlushing()

Constructor TdAndroidLogger(..., true) has the same effect too.

    TdAndroidLogger logger = new TdAndroidLogger("1Qaz2WSx3eDc4RfvBGt56yHnMjU78ik", true)

Calling stopAutoFlushing() or close() stop the repeatedly flushing.

### Cleaning up

When you finish to use TdAndroidLogger, you have to call close() in order to release the resources.

    logger.close()

