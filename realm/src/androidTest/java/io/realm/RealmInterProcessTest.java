package io.realm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;

import io.realm.entities.AllTypes;
import io.realm.services.InterProcessService;

public class RealmInterProcessTest extends AndroidTestCase {
    private Realm testRealm;
    private Messenger serviceMessenger;
    private Messenger receiverMessenger;
    private final CountDownLatch serviceLatch = new CountDownLatch(1);
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceMessenger = new Messenger(iBinder);
            serviceLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    // Helper handler to make it easy to interact with service process.
    // This handler run in test thread's Looper. When writing the test case, remember to call
    // Looper.loop() to start handling message.
    // Pass the first thing you want to run to the constructor which will be posted to the beginning
    // of the message queue.
    // Write the comments of the test case like this:
    // A-Z means steps running from service process.
    // 1-9xx means steps running from the main process.
    @SuppressLint("HandlerLeak") // SuppressLint bug, doesn't work
    private class InterProcessHandler extends Handler {
        // Timeout Watchdog. In case the service crashed or expected response is not returned.
        // It is very important to feed the dog after the expected message arrived.
        private final int timeout = 2000;
        private boolean timeoutFlag = true;
        private Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeoutFlag) {
                    assertTrue("Timeout happened", false);
                } else {
                    timeoutFlag = true;
                    postDelayed(timeoutRunnable, timeout);
                }
            }
        };

        protected void clearTimeoutFlag() {
            timeoutFlag = false;
        }

        protected void done() {
            Looper.myLooper().quit();
        }

        public InterProcessHandler(Runnable startRunnable) {
            super(Looper.myLooper());
            receiverMessenger = new Messenger(this);
            post(startRunnable);
            postDelayed(timeoutRunnable, timeout);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String error = bundle.getString(InterProcessService.BUNDLE_KEY_ERROR);
            if (error != null) {
                assertTrue(error, false);
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testRealm = Realm.getInstance(getContext());
        RealmConfiguration conf = testRealm.getConfiguration();
        testRealm.close();
        Realm.deleteRealm(conf);

        Intent intent = new Intent(getContext(), InterProcessService.class);
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        serviceLatch.await();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getContext().unbindService(serviceConnection);
    }

    private void triggerServiceStep(InterProcessService.Step step) {
        Message msg = Message.obtain(null, step.message);
        msg.replyTo = receiverMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            assertTrue(false);
        }
    }

    // 1. Main process create Realm, write one object.
    // A. Service process open Realm, check if there is one and only one object.
    public void testCreateInitialRealm() throws InterruptedException {
        new InterProcessHandler(new Runnable() {
            @Override
            public void run() {
                testRealm = Realm.getInstance(getContext());
                assertEquals(testRealm.allObjects(AllTypes.class).size(), 0);
                testRealm.beginTransaction();
                testRealm.createObject(AllTypes.class);
                testRealm.commitTransaction();

                triggerServiceStep(InterProcessService.stepCreateInitialRealm_A);
            }}) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == InterProcessService.stepCreateInitialRealm_A.message) {
                    clearTimeoutFlag();
                    done();
                } else {
                    assertTrue(false);
                }
            }
        };
        Looper.loop();
    }

    // A. Open Realm.
    // 1. Open Realm and add one object.
    // B. Call Realm.refresh and check if the added object in step 1 can be found.
    public void testAddCheckWithRefresh() {
        new InterProcessHandler(new Runnable() {
            @Override
            public void run() {
                triggerServiceStep(InterProcessService.stepAddCheckWithRefresh_A);
            }
        }) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == InterProcessService.stepAddCheckWithRefresh_A.message) {
                    testRealm = Realm.getInstance(getContext());
                    assertEquals(testRealm.allObjects(AllTypes.class).size(), 0);
                    testRealm.beginTransaction();
                    testRealm.createObject(AllTypes.class);
                    testRealm.commitTransaction();
                    assertEquals(testRealm.allObjects(AllTypes.class).size(), 1);
                    clearTimeoutFlag();
                    triggerServiceStep(InterProcessService.stepAddCheckWithRefresh_B);
                } else if (msg.what == InterProcessService.stepAddCheckWithRefresh_B.message) {
                    clearTimeoutFlag();
                    done();
                } else {
                    assertTrue(false);
                }
            }
        };
        Looper.loop();
    }
}
