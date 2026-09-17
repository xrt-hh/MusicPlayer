package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvSongName, tvCurrentTime, tvDuration;
    private SeekBar seekBar;
    private Button btnPlayPause, btnPrev, btnNext;
    private boolean isPlaying = false;
    private boolean isUserSeeking = false;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicService.BROADCAST_UPDATE.equals(intent.getAction())) {
                String songName = intent.getStringExtra(MusicService.EXTRA_SONG_NAME);
                isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false);
                int currentPosition = intent.getIntExtra(MusicService.EXTRA_CURRENT_POSITION, 0);
                int duration = intent.getIntExtra(MusicService.EXTRA_DURATION, 0);

                tvSongName.setText(songName);
                updatePlayPauseButton();
//进度条更新
                if (duration > 0) {
                    if (!isUserSeeking) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(currentPosition);
                    }
                    tvCurrentTime.setText(formatTime(currentPosition));
                    tvDuration.setText(formatTime(duration));
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSongName = findViewById(R.id.tv_song_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvDuration = findViewById(R.id.tv_duration);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_UPDATE);
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);


        startService(new Intent(this, MusicService.class));

        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                sendAction(MusicService.ACTION_PAUSE);
            } else {
                sendAction(MusicService.ACTION_PLAY);
            }
        });

        btnPrev.setOnClickListener(v -> sendAction(MusicService.ACTION_PREV));
        btnNext.setOnClickListener(v -> sendAction(MusicService.ACTION_NEXT));
//进度条点击事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                Intent seekIntent = new Intent(MainActivity.this, MusicService.class);
                seekIntent.setAction(MusicService.ACTION_SEEK);
                seekIntent.putExtra(MusicService.EXTRA_SEEK_POSITION, seekBar.getProgress());
                startService(seekIntent);
            }
        });
    }

    private void sendAction(String actionPause) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(actionPause);
        startService(intent);
    }
//mm:ss
    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updatePlayPauseButton()
    {
        btnPlayPause.setText(isPlaying ? "停止" : "播放");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}