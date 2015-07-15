package io.realm.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.entities.AllTypes;

/**
 * Helper service for multi-processes support testing.
 */
public class InterProcessService extends Service {

    public abstract static class Step {
        public final int message;
        private Step(int message) {
            this.message = message;
            stepMap.put(message, this);
        }

        abstract void run();

        protected void response(String error) {
            try {
                Message msg = Message.obtain(null, message);
                if (error != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(BUNDLE_KEY_ERROR, error);
                    msg.setData(bundle);
                }
                thiz.client.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static final String BUNDLE_KEY_ERROR = "error";
    private static Map<Integer, Step> stepMap = new HashMap<Integer, Step>();

    private static InterProcessService thiz;
    private Realm testRealm;

    private final Messenger messenger = new Messenger(new IncomingHandler());
    private Messenger client;

    public InterProcessService() {
        if (thiz != null) {
            throw new RuntimeException("Only one instance is allowed!");
        }
        thiz = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return super.onUnbind(intent);
    }

    private static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            thiz.client = msg.replyTo;
            if (thiz.client == null) {
                throw new RuntimeException("Message with an empty client.");
            }
            Step step = stepMap.get(msg.what);
            if (step != null) {
                step.run();
            } else {
                throw new RuntimeException("Cannot find corresponding step to message " + msg.what + ".");
            }
        }
    }


    private static String currentLine() {
        StackTraceElement element = new Throwable().getStackTrace()[1];
        return element.getClassName() + " line " + element.getLineNumber() + ": ";
    }

    public final static Step stepCreateInitialRealm_A = new Step(10) {

        @Override
        void run() {
            thiz.testRealm = Realm.getInstance(thiz);
            int expected = 1;
            int got = thiz.testRealm.allObjects(AllTypes.class).size();
            if (expected == got) {
                response(null);
            } else {
                response(currentLine() + "expected: " + expected + ", but got " + got);
            }
            thiz.testRealm.close();
        }
    };

    public final static Step stepAddCheckWithRefresh_A = new Step(20) {

        @Override
        public void run() {
            thiz.testRealm = Realm.getInstance(thiz);
            int expected = 0;
            int got = thiz.testRealm.allObjects(AllTypes.class).size();
            if (expected == got) {
                response(null);
            } else {
                response(currentLine() + "expected: " + expected + ", but got " + got);
            }
        }
    };

    public final static Step stepAddCheckWithRefresh_B = new Step(21) {

        @Override
        public void run() {
            int expected = 1;
            thiz.testRealm.refresh();
            int got = thiz.testRealm.allObjects(AllTypes.class).size();
            if (expected == got) {
                response(null);
            } else {
                response(currentLine() + "expected: " + expected + ", but got " + got);
            }
            thiz.testRealm.close();
        }
    };
}
