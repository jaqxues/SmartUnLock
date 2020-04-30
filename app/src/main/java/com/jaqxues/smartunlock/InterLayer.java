package com.jaqxues.smartunlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * The XposedManager class cannot be accessed in any way from an Activity as it implements an
 * interface from the XposedBridge. This Class is only resolved when Xposed initializes the class.
 * This means we cannot access anything in that specific class (i.e. no final String etc). To allow
 * this, we use this class that does not implement an at runtime unknown interface.
 */
public class InterLayer {
    public static final String REQUEST_TOGGLE_ACTION_ACTIVITY = "smartunlock.intent.action.toggle_mode.activity";
    public static final String REQUEST_TOGGLE_ACTION_SERVICE = "smartunlock.intent.action.toggle_mode.service";
    public static final String CALLBACK_BOOLEAN_EXTRA_ACTIVITY_REQUEST = "smartunlock.intent.action.request_relaunch_with_bool.activity";
    public static final String CALLBACK_BOOLEAN_EXTRA_SERVICE_REQUEST = "smartunlock.intent.action.request_relaunch_with_bool.service";
    public static final String CALLBACK_BOOLEAN_EXTRA_SERVICE_RESPONSE = "smartunlock.intent.action.response_relaunch_with_bool.service";
    public static final String CALLBACK_BOOLEAN_EXTRA_SERVICE_FORWARD = "smartunlock.intent.action.forward_relaunch_with_bool.service";
    public static final String INTENT_EXTRA_IS_ACTIVE = "smartunlock.intent.extra.is_active";

    public static final String TRUST_MANAGER_SERVICE_CLASS = "com.android.server.trust.TrustManagerService";

    private static Object trustManager;
    private static boolean updateTrustAlreadyCalled;
    private static boolean isActive = false;

    private static final BroadcastReceiver broadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case CALLBACK_BOOLEAN_EXTRA_ACTIVITY_REQUEST:
                    context.startActivity(
                            context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
                                    .putExtra(INTENT_EXTRA_IS_ACTIVE, isActive));
                    break;
                case CALLBACK_BOOLEAN_EXTRA_SERVICE_REQUEST:
                    context.startActivity(
                            context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
                                    .putExtra(INTENT_EXTRA_IS_ACTIVE, isActive)
                                    .putExtra(CALLBACK_BOOLEAN_EXTRA_SERVICE_FORWARD, true));
                    break;
                case REQUEST_TOGGLE_ACTION_ACTIVITY:
                    isActive = !isActive;
                    updateTrustAll();
                    break;
                case REQUEST_TOGGLE_ACTION_SERVICE:
                    isActive = !isActive;
                    updateTrustAll();
                    context.startActivity(
                            context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID)
                                    .putExtra(INTENT_EXTRA_IS_ACTIVE, isActive)
                                    .putExtra(CALLBACK_BOOLEAN_EXTRA_SERVICE_FORWARD, true));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown Intent Action");
            }
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
                        intentFilter.addAction(CALLBACK_BOOLEAN_EXTRA_ACTIVITY_REQUEST);
                        intentFilter.addAction(CALLBACK_BOOLEAN_EXTRA_SERVICE_REQUEST);
                        intentFilter.addAction(REQUEST_TOGGLE_ACTION_ACTIVITY);
                        intentFilter.addAction(REQUEST_TOGGLE_ACTION_SERVICE);
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
}
