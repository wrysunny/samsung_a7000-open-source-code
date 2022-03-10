package com.sec.android.app.fm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MonoAudioReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RadioPlayer player = RadioPlayer.getInstance();
        player.applyStereo();
    }
}