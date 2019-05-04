package com.jaqxues.smartunlock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedManager implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals("android") &&
                lpparam.processName.equals("android")) {
            XposedBridge.log("Initializing Android: Starting TrustedManagerService Hooks");
            try {
                InterLayer.hookTrustManager(lpparam.classLoader);
                InterLayer.hookWindowManager(lpparam.classLoader);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        } else if (lpparam.packageName.equals("com.jaqxues.smartunlock")) {
            XposedHelpers.findAndHookMethod(MainActivity.class.getName(), lpparam.classLoader,
                    "xposedModuleActive", XC_MethodReplacement.returnConstant(true));
        }
    }
}
