package com.jaqxues.smartunlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!xposedModuleActive()) {
            new AlertDialog.Builder(this)
                    .setTitle("Module not active")
                    .setMessage("This Xposed Module is not activated in the Xposed Installer. If you are experiencing this issue, and the module is active, make sure the Xposed Framework is installed correctly and works. Remember to perform a reboot after activating a module")
                    .setCancelable(false)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).show();
            return;
        }

        if (!getIntent().hasExtra(InterLayer.INTENT_EXTRA_IS_ACTIVE)) {
            Intent intent = new Intent(InterLayer.CALLBACK_BOOLEAN_EXTRA_ACTIVITY_REQUEST);
            sendBroadcast(intent);
            finish();
            return;
        }

        boolean isActive = getIntent().getExtras().getBoolean(InterLayer.INTENT_EXTRA_IS_ACTIVE);

        if (getIntent().getBooleanExtra(InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_FORWARD, false)) {
            Intent intent = new Intent(InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_RESPONSE);
            intent.putExtra(InterLayer.INTENT_EXTRA_IS_ACTIVE, isActive);
            sendBroadcast(intent);
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        if (isActive) {
            builder
                    .setTitle("Disable" + "SmartUnLock")
                    .setMessage("This will disable SmartUnLock: A Password / Pin / biometric Authentication will be needed again to unlock your phone.")
                    .setPositiveButton("Disable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            toggle();
                            finish();
                        }
                    });
        } else {
            builder
                    .setTitle("Enable SmartUnLock")
                    .setMessage("This will enable the Smart Unlock Mode: No Password / Pin / biometric Authentication will be needed to unlock your phone. \n\nBe aware that this mode will grant everyone access to your phone and disable your Android's security mechanisms.")
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            toggle();
                            finish();
                        }
                    });
        }
        builder.show();
    }

    private void toggle() {
        Intent intent = new Intent(InterLayer.REQUEST_TOGGLE_ACTION_ACTIVITY);
        sendBroadcast(intent);
    }

    private static boolean xposedModuleActive() {
        return false;
    }
}
