package com.treasuredata.android.cdp;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LookupResultTest {

    private CountDownLatch latch;

    @Before
    public void setup() {
        latch = new CountDownLatch(1);
    }

    private void await() throws InterruptedException {
        // Should be fast, we don't do IO in these tests
        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void should_success_with_a_json_array() throws Exception {
        LookupResult
                .create(200, "[" +
                        "  {" +
                        "    \"values\": [" +
                        "      \"123\"" +
                        "    ]," +
                        "    \"attributes\": {" +
                        "      \"x\": 1" +
                        "    }," +
                        "    \"key\": {" +
                        "      \"id\": \"abcd\"" +
                        "    }," +
                        "    \"audienceId\": \"234\"" +
                        "  }" +
                        "]")
                .invoke(shouldSuccess);
        await();
    }

    @Test
    public void should_success_with_an_empty_json_array() throws Exception {
        LookupResult.create(200, "[]").invoke(shouldSuccess);
        await();
    }

    @Test
    public void should_fail_with_an_json_object() throws Exception {
        LookupResult
                .create(200, "{" +
                        "    \"error\": \"Bad Request\"," +
                        "    \"message\": \"Some elaboration\"" +
                        "    \"status\": 400" +
                        "  }")
                .invoke(shouldFailedWith(new CDPAPIException(400, "Bad Request", "Some elaboration")));
        await();
    }

    @Test
    public void should_fail_on_a_json_object_even_without_error_and_message() throws Exception {
        LookupResult
                .create(401, "{}")
                .invoke(shouldFailedWith(new CDPAPIException(401, null, "{}")));
        await();
    }

    @Test
    public void should_fail_on_a_non_200_with_arbitrary_body() throws Exception {
        LookupResult
                .create(400, "<body>")
                .invoke(shouldFailedWith(new CDPAPIException(400, null, "<body>")));
        await();
    }

    @Test
    public void should_fail_on_a_non_200_even_with_valid_json_array_body() throws Exception {
        String body = "[" +
                "  {" +
                "    \"values\": [" +
                "      \"123\"" +
                "    ]," +
                "    \"attributes\": {" +
                "      \"x\": 1" +
                "    }," +
                "    \"key\": {" +
                "      \"id\": \"abcd\"" +
                "    }," +
                "    \"audienceId\": \"234\"" +
                "  }" +
                "]";
        LookupResult
                .create(400, body)
                .invoke(shouldFailedWith(new CDPAPIException(400, null, body)));
        await();
    }

    @Test
    public void should_fail_on_200_but_non_json_body() throws Exception {
        LookupResult
                .create(200, "<body>")
                .invoke(shouldFailedWith(new CDPAPIException(200, null, "<body>")));
        await();
    }

    @Test
    public void should_fail_on_200_but_empty_body() throws Exception {
        LookupResult
                .create(200, "")
                .invoke(shouldFailedWith(new CDPAPIException(200, null, "")));
        await();
    }

    @Test
    public void should_fail_on_200_json_but_not_segment_schema_body() throws Exception {
        String body = "{\"some_random\": \"json\"}";
        LookupResult
                .create(200, body)
                // Because we do parse then re-format internally,
                // the JSON format of exception's message will not stay same as the
                // original response but be normalized.
                .invoke(shouldFailedWith(new CDPAPIException(200, null, new JSONObject(body).toString())));
        await();
    }

    @Test
    public void should_fail_and_parse_error_and_message_on_200_with_json_error_schema() throws Exception {
        LookupResult
                .create(200, "{" +
                        "    \"error\": \"Bad Request\"," +
                        "    \"message\": \"Some elaboration\"" +
                        "}")
                .invoke(shouldFailedWith(new CDPAPIException(200, "Bad Request", "Some elaboration")));
        await();
    }

    private final FetchUserSegmentsCallback shouldSuccess = new FetchUserSegmentsCallback() {
        @Override
        public void onSuccess(List<Profile> profiles) {
            latch.countDown();
        }

        @Override
        public void onError(Exception e) {
            fail("Expect onError to be never get called!");
            latch.countDown();
        }
    };

    private final FetchUserSegmentsCallback shouldFailed = new FetchUserSegmentsCallback() {
        @Override
        public void onSuccess(List<Profile> profiles) {
            fail("Expect onSuccess to be get called!");
            latch.countDown();
        }

        @Override
        public void onError(Exception e) {
            latch.countDown();
        }
    };

    private FetchUserSegmentsCallback shouldFailedWith(final Exception expected) {
        return new FetchUserSegmentsCallback() {
            @Override
            public void onSuccess(List<Profile> profiles) {
                fail("Expect onSuccess to be get called!");
                latch.countDown();
            }


            @Override
            public void onError(Exception exception) {
                // Pseudo equality checking, should probably do a better matcher here
                assertTrue(exception.getClass().isAssignableFrom(expected.getClass()));
                assertEquals(expected.getMessage(), exception.getMessage());
                latch.countDown();
            }
        };
    }

    private FetchUserSegmentsCallback shouldFailedWith(final CDPAPIException expected) {
        return new FetchUserSegmentsCallback() {
            @Override
            public void onSuccess(List<Profile> profiles) {
                fail("Expect onSuccess to be get called!");
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                assertTrue(exception instanceof CDPAPIException);
                CDPAPIException CDPAPIException = (CDPAPIException) exception;
                assertEquals(expected.getMessage(), CDPAPIException.getMessage());
                assertEquals(expected.getError(), CDPAPIException.getError());
                assertEquals(expected.getStatus(), CDPAPIException.getStatus());
                latch.countDown();
            }
        };
    }

    private FetchUserSegmentsCallback shouldFailedWith(final String exceptionMessage) {
        return new FetchUserSegmentsCallback() {
            @Override
            public void onSuccess(List<Profile> profiles) {
                fail("Expect onSuccess to be get called!");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                assertEquals(exceptionMessage, e.getMessage());
                latch.countDown();
            }
        };
    }

}