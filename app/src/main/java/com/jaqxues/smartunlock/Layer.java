package com.jaqxues.smartunlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.KeyEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * The XposedManager class cannot be accessed in any way from an Activity as it implements an
 * interface from the XposedBridge. This Class is only resolved when Xposed initializes the class.
 * This means we cannot access anything in that specific class (i.e. no final String etc). To allow
 * this, we use this class that does not implement an at runtime unknown interface.
 */
public class Layer {

    public static final String ACTIVATED_SMARTUNLOCK_ACTION = "smartunlock.intent.action.toggle_mode";
    public static final String RELAUNCH_WITH_BOOLEAN_EXTRA_ACTION = "smartunlock.intent.action.relaunch_with_bool";

    public static final String TRUST_MANAGER_SERVICE_CLASS = "com.android.server.trust.TrustManagerService";
    public static final String PHONE_WINDOW_MANAGER_CLASS = (Build.VERSION.SDK_INT > 22) ?
            "com.android.server.policy.PhoneWindowManager" : "com.android.internal.policy.impl.PhoneWindowManager";

    private static Object trustManager;
    private static boolean updateTrustAlreadyCalled;
    private static boolean isActive = false;

    private static BroadcastReceiver broadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RELAUNCH_WITH_BOOLEAN_EXTRA_ACTION.equals(intent.getAction())) {
                launchUI();
                return;
            }

            isActive = !isActive;
            updateTrustAll();
        }
    };


    static void hookTrustManager(ClassLoader classLoader) {
        XposedBridge.hookAllConstructors(
                XposedHelpers.findClass(TRUST_MANAGER_SERVICE_CLASS, classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        trustManager = param.thisObject;

                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(ACTIVATED_SMARTUNLOCK_ACTION);
                        intentFilter.addAction(RELAUNCH_WITH_BOOLEAN_EXTRA_ACTION);
                        ((Context) XposedHelpers.getObjectField(trustManager, "mContext"))
                                .registerReceiver(broadCastReceiver, intentFilter);
                    }
                });

        XposedHelpers.findAndHookMethod(TRUST_MANAGER_SERVICE_CLASS, classLoader,
                "refreshAgentList", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        updateTrustAlreadyCalled = false;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!updateTrustAlreadyCalled) updateTrustAll();
                    }
                });

        XposedHelpers.findAndHookMethod(TRUST_MANAGER_SERVICE_CLASS, classLoader,
                "updateTrustAll", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        updateTrustAlreadyCalled = true;
                    }
                });

        XposedHelpers.findAndHookMethod(TRUST_MANAGER_SERVICE_CLASS, classLoader,
                "aggregateIsTrustManaged", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isActive) return;

                        if (!isTrustAllowedForUser((int) param.args[0]))
                            XposedBridge.log("User not (yet) allowed to use Trust Manager");
                        else param.setResult(true);
                    }
                });

        XposedHelpers.findAndHookMethod(TRUST_MANAGER_SERVICE_CLASS, classLoader,
                "aggregateIsTrusted", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isActive) return;

                        if (!isTrustAllowedForUser((int) param.args[0]))
                            XposedBridge.log("User not (yet) allowed to use Trust Manager");
                        else param.setResult(true);
                    }
                });
    }

    static void hookWindowManager(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(PHONE_WINDOW_MANAGER_CLASS, classLoader,
                "interceptKeyBeforeDispatching",
                "android.view.WindowManagerPolicy.WindowState",
                KeyEvent.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        KeyEvent keyEvent = (KeyEvent) param.args[1];
                        if (keyEvent.getScanCode() == 96 &&
                                keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getRepeatCount() == 50) {
                            XposedBridge.log("Matched Activation Conditions");
                            launchUI();
                        }
                    }
                });
    }

    private static boolean hasStrongAuthTracker() {
        try {
            XposedHelpers.findField(trustManager.getClass(), "mStrongAuthTracker");
            return true;
        } catch (NoSuchFieldError ignored) {}
        return false;
    }

    private static boolean isTrustAllowedForUser(int userId) {
        if (hasStrongAuthTracker()) {
            Object authTracker = XposedHelpers.getObjectField(trustManager, "mStrongAuthTracker");
            return (boolean) XposedHelpers.callMethod(authTracker,
                    "isTrustAllowedForUser", userId);
        } else {
            return (boolean) XposedHelpers.callMethod(trustManager,
                    "getUserHasAuthenticated", userId);
        }
    }

    private static void updateTrustAll() {
        try {
            XposedHelpers.callMethod(trustManager, "updateTrustAll");
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void launchUI() {
        Context context = (Context) XposedHelpers.getObjectField(trustManager, "mContext");
        context.startActivity(
                context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
                        .putExtra("isActive", isActive));
    }
}
