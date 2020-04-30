package com.jaqxues.smartunlock;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * This file was created by Jacques Hoffmann (jaqxues) in the Project SmartUnLock.<br>
 * Date: 29.04.20 - Time 12:19.
 */
@TargetApi(Build.VERSION_CODES.N)
public class QsTileService extends TileService {
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_RESPONSE.equals(intent.getAction())) {
                if (!intent.hasExtra(InterLayer.INTENT_EXTRA_IS_ACTIVE)) throw new IllegalArgumentException("No Extra");
                Tile tile = getQsTile();
                if (tile != null) {
                    updateQsState(tile, !intent.getExtras().getBoolean(InterLayer.INTENT_EXTRA_IS_ACTIVE));
                }
            }
        }
    };


    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_RESPONSE);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onTileAdded() {
        sendBroadcast(new Intent(InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_REQUEST));
    }

    @Override
    public void onStartListening() {
        sendBroadcast(new Intent(InterLayer.CALLBACK_BOOLEAN_EXTRA_SERVICE_REQUEST));
    }

    public void updateQsState(Tile qsTile, boolean isActive) {
        qsTile.setState(isActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        qsTile.setIcon(isActive ? Icon.createWithResource(this, R.drawable.ic_lock_open_black_24dp)
                : Icon.createWithResource(this, R.drawable.ic_lock_closed_black_24dp));
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        sendBroadcast(new Intent(InterLayer.REQUEST_TOGGLE_ACTION_SERVICE));
    }
}
