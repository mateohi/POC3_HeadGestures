package com.ic.banking.glass.poc3_headgestures;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;

public class HeadDetectionActivity extends Activity {

    private HeadGestureDetector headGestureDetector;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(buildView("test"));
        createHeadGestureDetector();
    }

    private void createHeadGestureDetector() {
        this.headGestureDetector = new HeadGestureDetector(getApplicationContext(), new HeadGestureListener() {
            @Override
            public void onNod() {
                headNod();
            }

            @Override
            public void onHeadShake() {
                Toast.makeText(getApplicationContext(), "Head shake", Toast.LENGTH_SHORT);
                playSound(Sounds.SUCCESS);
            }

            @Override
            public void onWink() {
                Toast.makeText(getApplicationContext(), "Wink", Toast.LENGTH_SHORT);
                playSound(Sounds.SUCCESS);
            }
        });
    }

    private void headNod() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Nod", Toast.LENGTH_SHORT);
                playSound(Sounds.SUCCESS);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.headGestureDetector != null) {
            this.headGestureDetector.startListening();
        }
    }

    @Override
    protected void onPause() {
        if (this.headGestureDetector != null) {
            this.headGestureDetector.stopListening();
        }
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            playSound(Sounds.DISALLOWED);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private View buildView(String text) {
        Card card = new Card(this);
        card.setText(text);

        return card.getView();
    }

    private void playSound(int sound) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(sound);
    }

}
