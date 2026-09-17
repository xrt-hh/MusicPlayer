package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    public static final String ACTION_PLAY = "com.example.musicplayer.PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayer.PAUSE";
    public static final String ACTION_NEXT = "com.example.musicplayer.NEXT";
    public static final String ACTION_PREV = "com.example.musicplayer.PREV";
    public static final String BROADCAST_UPDATE = "com.example.musicplayer.UPDATE_UI";
    public static final String EXTRA_SONG_NAME = "song_name";
    public static final String EXTRA_IS_PLAYING = "is_playing";
    public static final String EXTRA_CURRENT_POSITION = "current_position";
    public static final String EXTRA_DURATION = "duration";
    public static final String ACTION_SEEK = "com.example.musicplayer.SEEK";
    public static final String EXTRA_SEEK_POSITION = "seek_position";
    private MediaPlayer mediaPlayer;
    private List<Integer> songList;
    private String[] songNames;
    private int currentIndex=0;
    private boolean isPlaying=false;
    private Handler handler = new Handler();
    private Runnable progressUpdater;
    @Override
    public void onCreate(){
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        songList =new ArrayList<>();
        songList.add(R.raw.kangjiawu);
        songList.add(R.raw.shoupaigu);
        songList.add(R.raw.landiaoxiaohao);
        songList.add(R.raw.dajiyueqi);
        songNames = new String[]{"康加舞", "手拍鼓", "蓝调小号","打击乐器"};
        // 下一首
        mediaPlayer.setOnCompletionListener(mp -> nextSong(true));
        // 进度条更新
        progressUpdater =new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer!=null&&isPlaying){
                    sendUpdateBroadcast();
                    handler.postDelayed(this,500);
                }
            }
        };
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent!=null&&intent.getAction()!=null){
            switch (intent.getAction()){
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_NEXT:
                    nextSong(false);
                    break;
                case ACTION_PREV:
                    previousSong();
                    break;
                case ACTION_SEEK:
                    int seekPos = intent.getIntExtra(EXTRA_SEEK_POSITION, -1);
                    if (seekPos >= 0) {
                        //跳转进度条
                        seekTo(seekPos);
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private void previousSong() {
        if (songList.isEmpty()) return;
        if (mediaPlayer.getCurrentPosition() <= 2000) {
            currentIndex = (currentIndex - 1 + songList.size()) % songList.size();
            try {
                mediaPlayer.reset();
                AssetFileDescriptor afd = getResources().openRawResourceFd(songList.get(currentIndex));
                try {
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                } finally {
                    afd.close();
                }
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                Log.e("MusicService", "切换上一曲出错", e);
            }
        } else {
            mediaPlayer.seekTo(0);
        }
        isPlaying = true;
        sendUpdateBroadcast();
        handler.post(progressUpdater);
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            handler.removeCallbacks(progressUpdater);
            sendUpdateBroadcast();
        }
    }

    private void play() {
        if (mediaPlayer == null) return;
        if (!isPlaying) {
            if (mediaPlayer.isPlaying()) return;
            try {
                if (mediaPlayer.getCurrentPosition() > 0) {
                    mediaPlayer.start();
                } else {
                    mediaPlayer.reset();
                    // 重置进度条
                    AssetFileDescriptor afd = getResources().openRawResourceFd(songList.get(currentIndex));
                    try {
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    } finally {
                        afd.close();
                    }
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
                isPlaying = true;
                sendUpdateBroadcast();
                handler.post(progressUpdater);
            } catch (Exception e) {
                Log.e("MusicService", "播放出错", e);
            }
        }
    }

// 发送更新广播
    private void sendUpdateBroadcast() {
        if (mediaPlayer == null) return;
        Intent intent = new Intent(BROADCAST_UPDATE);
        intent.putExtra(EXTRA_SONG_NAME, songNames[currentIndex]);
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);
        intent.putExtra(EXTRA_CURRENT_POSITION, mediaPlayer.getCurrentPosition());
        intent.putExtra(EXTRA_DURATION, mediaPlayer.getDuration());
        sendBroadcast(intent);
    }

    private void nextSong(boolean b) {
        if (songList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % songList.size();
        try {
            mediaPlayer.reset();
            AssetFileDescriptor afd = getResources().openRawResourceFd(songList.get(currentIndex));
            try {
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } finally {
                afd.close();
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            sendUpdateBroadcast();
            handler.post(progressUpdater);
        } catch (Exception e) {
            Log.e("MusicService", "切换歌曲出错", e);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressUpdater);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            sendUpdateBroadcast();
        }
    }
}
