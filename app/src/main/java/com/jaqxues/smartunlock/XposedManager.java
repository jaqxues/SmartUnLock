package com.jaqxues.smartunlock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedManager implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("android") &&
                lpparam.processName.equals("android")) {
            XposedBridge.log("Initializing Android: Starting TrustedManagerService Hooks");
            try {
                Layer.hookTrustManager(lpparam.classLoader);
                Layer.hookWindowManager(lpparam.classLoader);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }
}
