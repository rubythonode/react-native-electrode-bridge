package com.walmartlabs.electrode.reactnative.bridge;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.walmartlabs.electrode.reactnative.sample.model.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static com.walmartlabs.electrode.reactnative.bridge.BridgeMessage.BRIDGE_MSG_DATA;

public class ElectrodeBridgeTransceiverTest extends BaseBridgeTestCase {

    public void testSendRequestForTimeOut() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();

        ElectrodeBridgeRequest electrodeBridgeRequest = new ElectrodeBridgeRequest.Builder("sampleRequest").build();

        electrodeNativeBridge.sendRequest(electrodeBridgeRequest, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {
                assertNotNull(failureMessage);
                countDownLatch.countDown();
            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse responseData) {
                fail();
            }
        });
        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testSendRequestWithEmptyRequestDataAndNonEmptyResponse() {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final String expectedResult = "yay tests";
        ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();

        electrodeNativeBridge.registerRequestHandler("sampleRequest", new ElectrodeBridgeRequestHandler<ElectrodeBridgeRequest, Object>() {
            @Override
            public void onRequest(@Nullable ElectrodeBridgeRequest payload, @NonNull ElectrodeBridgeResponseListener<Object> responseListener) {
                assertNotNull(payload);
                assertNull(payload.getData());
                assertNotNull(responseListener);
                responseListener.onSuccess(expectedResult);
                countDownLatch.countDown();
            }
        });


        ElectrodeBridgeRequest electrodeBridgeRequest = new ElectrodeBridgeRequest.Builder("sampleRequest").build();
        electrodeNativeBridge.sendRequest(electrodeBridgeRequest, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {
                fail();
            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse response) {
                assertNotNull(response);
                assertEquals(expectedResult, response.getData());
                countDownLatch.countDown();
            }
        });
        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testSendRequestWithRequestDataAndEmptyResponse() {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final String expectedInput = "expectedInput";
        ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();

        electrodeNativeBridge.registerRequestHandler("sampleRequest", new ElectrodeBridgeRequestHandler<ElectrodeBridgeRequest, Object>() {
            @Override
            public void onRequest(@Nullable ElectrodeBridgeRequest payload, @NonNull ElectrodeBridgeResponseListener<Object> responseListener) {
                assertNotNull(payload);
                assertEquals(expectedInput, payload.getData());
                assertNotNull(responseListener);
                responseListener.onSuccess(null);
                countDownLatch.countDown();
            }
        });


        ElectrodeBridgeRequest electrodeBridgeRequest = new ElectrodeBridgeRequest.Builder("sampleRequest").withData(expectedInput).build();
        electrodeNativeBridge.sendRequest(electrodeBridgeRequest, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {
                fail();
            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse bridgeResponse) {
                assertNotNull(bridgeResponse);
                assertNull(bridgeResponse.getData());
                countDownLatch.countDown();
            }
        });

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testSendRequestWithRequestDataAndEmptyResponseWithJSRequestHandler() {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final String expectedInput = "expectedInput";
        final String REQUEST_NAME = "testSendRequestWithRequestDataAndEmptyResponseWithJSRequestHandler";
        ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();

        UUID uuid = addMockEventListener(REQUEST_NAME, new TestMockEventListener() {
            @Override
            public void onRequest(ReadableMap request, @NonNull MockJsResponseDispatcher jsResponseDispatcher) {
                assertNotNull(request);
                assertEquals(REQUEST_NAME, request.getString(ElectrodeBridgeRequest.BRIDGE_MSG_NAME));
                assertNotNull(jsResponseDispatcher);
                assertEquals(expectedInput, request.getString(BRIDGE_MSG_DATA));
                jsResponseDispatcher.dispatchResponse(Arguments.createMap());
                countDownLatch.countDown();
            }
        });

        ElectrodeBridgeRequest electrodeBridgeRequest = new ElectrodeBridgeRequest.Builder(REQUEST_NAME).withData(expectedInput).build();
        electrodeNativeBridge.sendRequest(electrodeBridgeRequest, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {
                fail();
            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse responseData) {
                assertNotNull(responseData);
                assertNotNull(responseData.getData());
                assertTrue(responseData.getData() instanceof Bundle);
                assertTrue(((Bundle) responseData.getData()).isEmpty());
                countDownLatch.countDown();
            }
        });

        waitForCountDownToFinishOrFail(countDownLatch);
        removeMockEventListener(uuid);
    }


    public void testEmitEventWithSimpleDataFromNative() {
        final String TEST_EVENT_NAME = "testEmitEventWithData";
        final String TEST_EVENT_VALUE = "this is a test event";

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();
        electrodeNativeBridge.addEventListener(TEST_EVENT_NAME, new ElectrodeBridgeEventListener<ElectrodeBridgeEvent>() {
            @Override
            public void onEvent(@Nullable ElectrodeBridgeEvent event) {
                assertNotNull(event);
                assertEquals(TEST_EVENT_VALUE, event.getData());
                countDownLatch.countDown();
            }
        });

        addMockEventListener(TEST_EVENT_NAME, new TestMockEventListener() {
            @Override
            public void onEvent(ReadableMap event) {
                assertNotNull(event);
                assertEquals(TEST_EVENT_NAME, event.getString(ElectrodeBridgeEvent.BRIDGE_MSG_NAME));
                assertTrue(event.hasKey(BRIDGE_MSG_DATA));
                assertEquals(TEST_EVENT_VALUE, event.getString(BRIDGE_MSG_DATA));
                countDownLatch.countDown();
            }
        });

        electrodeNativeBridge.sendEvent(new ElectrodeBridgeEvent.Builder(TEST_EVENT_NAME).withData(TEST_EVENT_VALUE).build());
        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testEmitEventWithSimpleDataFromJS() {
        final String TEST_EVENT_NAME = "testEmitEventWithSimpleDataFromJS";
        final String TEST_EVENT_VALUE = "this is a test event";
        final WritableMap eventMap = createTestEventMap(TEST_EVENT_NAME);
        eventMap.putString(BRIDGE_MSG_DATA, TEST_EVENT_VALUE);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();
        electrodeNativeBridge.addEventListener(TEST_EVENT_NAME, new ElectrodeBridgeEventListener<ElectrodeBridgeEvent>() {
            @Override
            public void onEvent(@Nullable ElectrodeBridgeEvent eventPayload) {
                assertNotNull(eventPayload);
                assertEquals(TEST_EVENT_VALUE, eventPayload.getData());
                countDownLatch.countDown();
            }
        });


        ElectrodeBridgeTransceiver.instance().sendMessage(eventMap);

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testEmitEventWithComplexDataFromNative() {
        final String TEST_EVENT_NAME = "testEmitEventWithComplexDataFromNative";
        final Person person = new Person.Builder("Richard Lemaire", 10).build();

        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();
        electrodeNativeBridge.addEventListener(TEST_EVENT_NAME, new ElectrodeBridgeEventListener<ElectrodeBridgeEvent>() {
            @Override
            public void onEvent(@Nullable ElectrodeBridgeEvent eventPayload) {
                assertNotNull(eventPayload);
                assertTrue(eventPayload.getData() instanceof Person);
                Person personResult = (Person) eventPayload.getData();
                assertNotNull(personResult);
                assertEquals(person, personResult);
                countDownLatch.countDown();
            }
        });

        addMockEventListener(TEST_EVENT_NAME, new TestMockEventListener() {
            @Override
            public void onEvent(ReadableMap event) {
                assertNotNull(event);
                assertEquals(TEST_EVENT_NAME, event.getString(ElectrodeBridgeEvent.BRIDGE_MSG_NAME));
                assertTrue(event.hasKey(BRIDGE_MSG_DATA));
                ReadableMap personMap = event.getMap(BRIDGE_MSG_DATA);
                assertEquals(person.getName(), personMap.getString("name"));
                assertEquals(person.getMonth(), Integer.valueOf((int) personMap.getDouble("month")));
                countDownLatch.countDown();
            }
        });

        electrodeNativeBridge.sendEvent(new ElectrodeBridgeEvent.Builder(TEST_EVENT_NAME).withData(person).build());

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testEmitEventWithComplexDataFromJS() {
        final String TEST_EVENT_NAME = "testEmitEventWithComplexDataFromJS";
        final Person person = new Person.Builder("Richard Lemaire", 10).build();
        final WritableMap eventMap = createTestEventMap(TEST_EVENT_NAME);
        eventMap.putMap(BRIDGE_MSG_DATA, Arguments.fromBundle(person.toBundle()));

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        final ElectrodeNativeBridge electrodeNativeBridge = ElectrodeBridgeTransceiver.instance();
        electrodeNativeBridge.addEventListener(TEST_EVENT_NAME, new ElectrodeBridgeEventListener<ElectrodeBridgeEvent>() {
            @Override
            public void onEvent(@Nullable ElectrodeBridgeEvent eventPayload) {
                assertNotNull(eventPayload);
                assertTrue(eventPayload.getData() instanceof Bundle);
                Bundle eventData = (Bundle) eventPayload.getData();
                assertNotNull(eventData);
                assertNotNull(eventData.getString("name"));
                assertEquals(person.getName(), eventData.getString("name"));
                countDownLatch.countDown();
            }
        });

        ElectrodeBridgeTransceiver.instance().sendMessage(eventMap);

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testEmitEventWithNoData() {

    }

    public void testGetEmptyArrayFromJsToNative() {
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        addMockEventListener("getEmptyArray", new TestMockEventListener() {
            @Override
            public void onRequest(ReadableMap request, @NonNull MockJsResponseDispatcher jsResponseDispatcher) {
                String[] emptyArray = new String[0];
                WritableMap map = Arguments.createMap();
                WritableArray array = Arguments.fromArray(emptyArray);
                map.putArray(BRIDGE_MSG_DATA, array);
                jsResponseDispatcher.dispatchResponse(map);
                countDownLatch.countDown();
            }
        });


        ElectrodeBridgeRequest request = new ElectrodeBridgeRequest.Builder("getEmptyArray").build();
        ElectrodeBridgeTransceiver.instance().sendRequest(request, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {

            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse responseData) {
                assertNotNull(responseData);
                countDownLatch.countDown();
            }
        });

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testGetArrayFromJsToNative() {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final String[] stringArray = {"one", "two", "three"};
        addMockEventListener("getEmptyArray", new TestMockEventListener() {
            @Override
            public void onRequest(ReadableMap request, @NonNull MockJsResponseDispatcher jsResponseDispatcher) {
                WritableMap map = Arguments.createMap();
                WritableArray array = Arguments.fromArray(stringArray);
                map.putArray(BRIDGE_MSG_DATA, array);
                jsResponseDispatcher.dispatchResponse(map);
                countDownLatch.countDown();
            }
        });


        ElectrodeBridgeRequest request = new ElectrodeBridgeRequest.Builder("getEmptyArray").build();
        ElectrodeBridgeTransceiver.instance().sendRequest(request, new ElectrodeBridgeResponseListener<ElectrodeBridgeResponse>() {
            @Override
            public void onFailure(@NonNull FailureMessage failureMessage) {

            }

            @Override
            public void onSuccess(@Nullable ElectrodeBridgeResponse bridgeResponse) {
                assertNotNull(bridgeResponse);
                assertTrue(bridgeResponse.getData() instanceof String[]);
                String[] response = (String[]) bridgeResponse.getData();
                assertNotNull(response);
                assertEquals(stringArray.length, response.length);
                countDownLatch.countDown();
            }
        });

        ElectrodeBridgeHolder.addConstantsProvider(new ConstantsProvider() {
            @Nullable
            @Override
            public Map<String, Object> getConstants() {
                //Apps logic here.
                return null;
            }
        });

        waitForCountDownToFinishOrFail(countDownLatch);
    }

    public void testConstantProviders() {
        assertTrue(getNativeBridge() instanceof ReactContextBaseJavaModule);
        assertNull(((ReactContextBaseJavaModule) getNativeBridge()).getConstants());
        final CountDownLatch countDownLatch = new CountDownLatch(3);


        final Map<String, Object> constants1 = new HashMap() {{
            put("map1key1", "value1");
        }};

        final Map<String, Object> constants2 = new HashMap() {{
            put("map2key1", 10);
        }};

        new Thread(new Runnable() {
            @Override
            public void run() {
                ElectrodeBridgeHolder.addConstantsProvider(new ConstantsProvider() {
                    @Nullable
                    @Override
                    public Map<String, Object> getConstants() {
                        countDownLatch.countDown();
                        return constants1;
                    }
                });
            }
        }).run();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ElectrodeBridgeHolder.addConstantsProvider(new ConstantsProvider() {
                    @Nullable
                    @Override
                    public Map<String, Object> getConstants() {
                        countDownLatch.countDown();
                        return constants2;
                    }
                });
            }
        }).run();


        ElectrodeBridgeHolder.addConstantsProvider(new ConstantsProvider() {
            @Nullable
            @Override
            public Map<String, Object> getConstants() {
                countDownLatch.countDown();
                return null;
            }
        });


        final Map<String, Object> expectedMap = ((ReactContextBaseJavaModule) getNativeBridge()).getConstants();
        assertNotNull(expectedMap);
        assertEquals(expectedMap.size(), (constants1.size() + constants2.size()));
        for (Map.Entry<String, Object> entry : constants1.entrySet()) {
            assertTrue(expectedMap.containsKey(entry.getKey()));
        }
        for (Map.Entry<String, Object> entry : constants2.entrySet()) {
            assertTrue(expectedMap.containsKey(entry.getKey()));
        }

        waitForCountDownToFinishOrFail(countDownLatch);
    }
}