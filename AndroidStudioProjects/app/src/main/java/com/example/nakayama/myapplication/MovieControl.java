package com.example.nakayama.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.view.View;

import java.io.File;
import java.io.Flushable;
import java.util.Timer;
import java.util.TimerTask;

import com.example.nakayama.myapplication.Types.ScreenState;
import com.example.nakayama.myapplication.Types.PlayState;

/**
 * Created by Nakayama on 2016/09/12.
 */
public class MovieControl
{
    private View BaseView;

    // VideoView オブジェクト
    private VideoView Video = null;

    // イベントリスナー
    private MovieListener Listener;

    // コントロール表示クリアタイマー
    private Timer PlayControlClearTimer = null;
    private Handler PlayControlClearHandler = new Handler();

    // シークバー再生状況取得タイマー
    private Timer SeekVerTimer = null;
    private Handler SeekVerHandler = new Handler();

    // ダウンロードスレッド
    private AsyncFileDownload Afd;

    // ダウンロード状況取得タイマー
    private Timer DownLoadCheckTimer = null;
    private Handler DownLoadCheckHandler = new Handler();

    // チャタリングチェック用
    private int MovieEvent = 0;
    private float TouchPosition = 0;

    // 再生状態
    private ScreenState NowScreen = ScreenState.MINI;
    private PlayState NowPlaySt =  PlayState.STOP;
    private String NowUrl;
    private String NowTitle;

    // ストリーム再生チェック用
    private boolean StreamFlag = false;

    // 再生開始位置
    private int FirstPosition = 0;

    // コントロールアイテム一式
    private TextView playTypeTxt;
    private TextView statusTxt;
    private Button playButton;
    private Button nextButton;
    private Button prevButton;
    private Button m15Button;
    private Button p15Button;
    private Button screenButton;
    private SeekBar seekBar;

    // コンストラクタ
    public MovieControl(View view)
    {
        BaseView = view;

        Video = (VideoView) view.findViewById(R.id.videoView);
        playTypeTxt = (TextView) BaseView.findViewById(R.id.playType_txt);
        statusTxt = (TextView) BaseView.findViewById(R.id.status_txt);
        playButton = (Button) BaseView.findViewById(R.id.play_button);
        nextButton = (Button) BaseView.findViewById(R.id.next_button);
        prevButton = (Button) BaseView.findViewById(R.id.prev_button);
        m15Button = (Button) BaseView.findViewById(R.id.m15_button);
        p15Button = (Button) BaseView.findViewById(R.id.p15_button);
        screenButton = (Button) BaseView.findViewById(R.id.screen_button);
        seekBar = (SeekBar) BaseView.findViewById(R.id.playVer);

        // 再生準備完了イベント設定
        Video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onPlayReady();
            }
        });

        // ビデオ押下イベント設定
        Video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                if(IsChatterCheck(ev)) {
                    if (ev.getAction() == MotionEvent.ACTION_MOVE) {

                        // 画面スライド
                        Listener.onMovieSlide();
                    }
                    else if (ev.getAction() == MotionEvent.ACTION_UP) {

                        // 画面タッチ
                        Listener.onMovieTouch();
                    }
                }

                return true;
            }
        });

        // 再生終了イベント設定
        Video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 再生停止
                onPlayEnd();
            }
        });

        // 再生ボタン押下イベント設定
        if (null != playButton) {
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(NowPlaySt == PlayState.PLAYING) {
                        onPauseButton();
                    }
                    else {
                        onPlayButton();
                    }
                }
            });
        }

        // 次へボタン押下イベント設定
        if (null != nextButton) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Listener.onPlayNext();
                }
            });
        }

        // 戻るボタン押下イベント設定
        if (null != prevButton) {
            prevButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Listener.onPlayPrev();
                }
            });
        }

        // 15秒戻るボタンクリック
        if (null != m15Button) {
            m15Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    on15SecPrevButton();
                }
            });
        }

        // 15秒進むボタンクリック
        if (null != p15Button) {
            p15Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    on15SecNextButton();
                }
            });
        }

        // スクリーンボタンクリック
        if (null != screenButton) {
            screenButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Listener.onScreenButton();
                }
            });
        }

        // シークバー位置移動イベント設定
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    // スライドイベント
                    public void onProgressChanged(
                            SeekBar seekBar, int progress, boolean fromUser) {
                    }

                    // ツマミタッチイベント
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        onSeekVarMove();
                    }

                    // ツマミ離したイベント
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        onSeekVarSelect(seekBar.getProgress());
                    }
                }
        );
    }

    // Viewの取得
    public VideoView GetView()
    {
        return Video;
    }

    // 再生開始制御
    public void PlayStart(String url, String path, int pos)
    {
        Log.d("MainActivity", "PlayStart()");

        FileDownloadCancel();

        Video.pause();

        if(null == path) {
            Log.d("MainActivity", "network play");
            Video.setVideoURI(Uri.parse(url));
            StreamFlag = true;
        }
        else {
            // ローカルファイルで再生
            Log.d("MainActivity", "local play");
            Video.setVideoPath(path);
            StreamFlag = false;
        }

        FirstPosition = pos;

        ViewUpdate(NowScreen, PlayState.LOADING);
    }

    // 再生停止制御
    public void PlayStop()
    {
        Log.d("MainActivity", "PlayStop()");

        Stop();

        FileDownloadCancel();

        if(NowUrl == null) {
            // 再生データがない場合は無視
            return;
        }

        ViewUpdate(NowScreen, PlayState.PAUSE);
    }

    // VideoView終了処理
    public void Close()
    {
        Stop();

        LoadingTimerStop();
        SeekVerTimerStop();
        ButtonDeleteTimerStop();

        Video = null;
    }

    // 動画エリア更新処理
    public void ViewUpdate(ScreenState screenSt, PlayState playSt)
    {
        ButtonDeleteTimerStop();

        NowScreen = screenSt;
        if(playSt != PlayState.NOT_CHANGE) {
            NowPlaySt = playSt;
        }


        // 制御ボタン更新
        ButtonUpdate(NowScreen, NowPlaySt);

        // シークバー更新
        SeekVerUpdate(NowScreen, NowPlaySt);
    }

    // ファイルダウンロード処理
    public void FileDownload(
            Activity owner, String title, String url, String path)
    {
        Stop();

        File directory = new File(
                owner.getFilesDir().getAbsolutePath()+ "/SampleFolder");
        if(directory.exists() == false) {
            directory.mkdir();
        }

        NowUrl = url;
        NowTitle = title;

        File outputFile = new File(path);

        Afd = new AsyncFileDownload(owner, url, outputFile);
        Afd.execute();

        ViewUpdate(NowScreen, PlayState.DOWNLOADING);

        // ローディング開始タイマースタート
        LoadingTimer(100);
    }

    // ファイルダウンロードキャンセル
    public void FileDownloadCancel()
    {
        if(Afd != null
                && Afd.getStatus() == AsyncFileDownload.Status.RUNNING) {

            Log.d("MainActivity", "download cancel for play stop");
            Afd.LordingCancel();
        }
    }

    //リスナー追加用
    public void SetMovieListener(MovieListener listener)
    {
        Listener = listener;
    }

    // [イベント]Play準備完了イベント処理
    public void onPlayReady()
    {
        Play(FirstPosition);

        // 画面更新
        ViewUpdate(NowScreen, PlayState.PLAYING);
    }

    // [イベント]再生終了イベント
    public void onPlayEnd()
    {
        // 再生停止
        Stop();

        // 現在のスクリーン状態を更新
        ViewUpdate(NowScreen, PlayState.STOP);
    }

    // [イベント]Playボタンクリック処理
    public void onPlayButton()
    {
        if(NowPlaySt == PlayState.PAUSE) {
            // 再生開始
            Play(-1);

            // 現在のスクリーン状態を更新
            ViewUpdate(NowScreen, PlayState.PLAYING);
        }
        if(NowPlaySt == PlayState.STOP) {
            // 終端まで行っていたら先頭から再生開始
            Play(0);

            // 現在のスクリーン状態を更新
            ViewUpdate(NowScreen, PlayState.PLAYING);
        }
    }

    // [イベント]Stopボタンクリック処理
    public void onPauseButton()
    {
        // 再生停止
        Stop();

        // 現在のスクリーン状態を更新
        ViewUpdate(NowScreen, PlayState.PAUSE);
    }

    // [イベント]15秒進むボタンクリック処理
    public void on15SecNextButton()
    {
        // 一旦停止
        Stop();

        // 位置を指定して再生再開
        Play(Video.getCurrentPosition() + (15 * 1000));

        // 現在のスクリーン状態を更新
        ViewUpdate(NowScreen, PlayState.PLAYING);
    }

    // [イベント]15秒戻るボタンクリック処理
    public void on15SecPrevButton()
    {
        // 一旦停止
        Stop();

        // 位置を指定して再生再開
        Play(Video.getCurrentPosition() - (15 * 1000));

        // 現在のスクリーン状態を更新
        ViewUpdate(NowScreen, PlayState.PLAYING);
    }

    // [イベント]シーク移動イベント
    public void onSeekVarMove()
    {
        if(Afd != null && Afd.IsDownloading()) {
            return ;
        }

        // 再生停止
        Stop();
    }

    // [イベント] シークバー位置確定イベント
    public void onSeekVarSelect(int pos)
    {
        if(Afd != null && Afd.IsDownloading()) {
            return ;
        }
        // 再生開始
        Play(pos);

        // 現在のスクリーン状態を更新
        ViewUpdate(NowScreen, PlayState.PLAYING);
    }

    // [イベント]ダウンロード完了イベント
    private void funcDownloadComplete(String url)
    {
        TextView statusTxt = (TextView) BaseView.findViewById(R.id.status_txt);

        statusTxt.setText("ダウンロード完了");

        Listener.onDownloadComplete(url);

    }

    // 動画再生
    private void Play(int seekPos)
    {
        // 指定位置へ移動
        if(0 <= seekPos) {
            Video.seekTo(seekPos);
        }

        // 再生
        Video.start();
    }

    // 動画停止
    private void Stop()
    {
        if(Video.isPlaying()) {
            Video.pause();
        }
    }

    // チャタリングチェック
    private boolean IsChatterCheck(MotionEvent ev)
    {
        int evt = ev.getAction();
        boolean enable = true;

        if((evt != MotionEvent.ACTION_MOVE)
                && (evt != MotionEvent.ACTION_UP)) {
            return false;
        }

        long currentTimeMillis = System.currentTimeMillis();

        if(evt == MotionEvent.ACTION_MOVE) {
            // MOVEイベント受信

            if(MovieEvent == MotionEvent.ACTION_MOVE) {
                // 前回もMOVEイベント受信

                // 前回と比較
                if(TouchPosition == 0) {
                    // MOVEイベント確定直後はUPを受けるまで無視
                    enable = false;
                }
                // 移動距離測定
                else if(ev.getX() - TouchPosition < 20.0) {
                    enable = false;
                }
                else {
                    // MOVEイベント確定
                    TouchPosition = 0;
                }
            }
            else {
                // 最初のMOVEイベント受信

                // 初期位置をセット
                TouchPosition = ev.getX();
                enable = false;
            }
        }
        else {
            // UPイベント受信
            if((TouchPosition == 0)
                    && (MovieEvent == MotionEvent.ACTION_MOVE)) {
                // 前回MOVEイベントが確定していれば直後を無視
                enable = false;
            }
        }

        MovieEvent = evt;

        return enable;
    }

    // 動画制御ボタンの表示制御
    private void ButtonUpdate(ScreenState screenSt, PlayState playSt)
    {
        if(ScreenState.MINI == screenSt) {

            playTypeTxt.setVisibility(View.INVISIBLE);
            nextButton.setVisibility(View.INVISIBLE);
            prevButton.setVisibility(View.INVISIBLE);
            p15Button.setVisibility(View.INVISIBLE);
            m15Button.setVisibility(View.INVISIBLE);
            playButton.setVisibility(View.INVISIBLE);
            screenButton.setVisibility(View.INVISIBLE);
            statusTxt.setVisibility(View.VISIBLE);

            if(PlayState.LOADING == playSt) {
                statusTxt.setText("...");
            }
            else if(PlayState.DOWNLOADING == playSt) {
                statusTxt.setText("↓");
            }
            else {
                statusTxt.setVisibility(View.INVISIBLE);
            }
        }
        else {
            if(PlayState.LOADING != playSt && PlayState.DOWNLOADING != playSt) {
                playTypeTxt.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
                prevButton.setVisibility(View.VISIBLE);
                p15Button.setVisibility(View.VISIBLE);
                m15Button.setVisibility(View.VISIBLE);
                playButton.setVisibility(View.VISIBLE);
                screenButton.setVisibility(View.VISIBLE);

                statusTxt.setVisibility(View.INVISIBLE);

                if(PlayState.PLAYING == playSt) {
                    playButton.setText("■");
                }
                else {
                    playButton.setText("▶");
                }

                if(StreamFlag) {
                    playTypeTxt.setText("ストリーミング再生");
                }
                else {
                    playTypeTxt.setText("ローカルファイル再生");
                }
            }
            else {
                nextButton.setVisibility(View.INVISIBLE);
                prevButton.setVisibility(View.INVISIBLE);
                p15Button.setVisibility(View.INVISIBLE);
                m15Button.setVisibility(View.INVISIBLE);
                playButton.setVisibility(View.INVISIBLE);
                screenButton.setVisibility(View.INVISIBLE);

                statusTxt.setVisibility(View.VISIBLE);

                if(PlayState.LOADING == playSt) {
                    statusTxt.setText("ロード中...");
                }
                else if(PlayState.DOWNLOADING == playSt) {
                    statusTxt.setText("ダウンロード中...");

                    if(NowTitle != null) {
                        playTypeTxt.setText(NowTitle);
                    }
                    else {
                        playTypeTxt.setText("");
                    }
                }
            }

            if(PlayState.LOADING != playSt && PlayState.DOWNLOADING != playSt) {
                // 動画制御ボタン表示タイマー開始
                ButtonDeleteTimer(5);
            }
        }
    }

    // ミニ画面のクリア
    public void MiniViewClear()
    {
        PlayStop();

        // 動画非表示
        Video.setVisibility(View.INVISIBLE);

        // ステータス表示クリア
        statusTxt.setVisibility(View.INVISIBLE);

    }

    // 動画制御ボタン関連タイマー開始
    private void ButtonDeleteTimer(long second)
    {
        if(null != PlayControlClearTimer) {
            PlayControlClearTimer.cancel();
            PlayControlClearTimer = null;
        }

        PlayControlClearTimer = new Timer();
        PlayControlClearTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                PlayControlClearHandler.post(new Runnable() {
                    public void run() {
                        // ボタンを非表示
                        ButtonClear();
                    }
                });
            }
        }, second * 1000);
    }

    // 動画制御ボタン関連タイマー停止
    private void ButtonDeleteTimerStop()
    {
        if(null != PlayControlClearTimer) {
            PlayControlClearTimer.cancel();
            PlayControlClearTimer = null;
        }
    }

    // 動画制御ボタンの表示クリア
    private void ButtonClear()
    {
        playButton.setVisibility(View.INVISIBLE);
        nextButton.setVisibility(View.INVISIBLE);
        prevButton.setVisibility(View.INVISIBLE);
        m15Button.setVisibility(View.INVISIBLE);
        p15Button.setVisibility(View.INVISIBLE);
        seekBar.setVisibility(View.INVISIBLE);
        screenButton.setVisibility(View.INVISIBLE);
        playTypeTxt.setVisibility(View.INVISIBLE);

        SeekVerTimerStop();
    }

    // シークバーの表示制御
    private void SeekVerUpdate(ScreenState screenSt, PlayState playSt)
    {
        SeekVerTimerStop();

        if(screenSt != ScreenState.MINI) {

            if(playSt == PlayState.DOWNLOADING) {
                // ダウンロード状況を表示
                seekBar.setMax(100);
                seekBar.setProgress(Afd.GetLoadedBytePercent());
            }
            else {
                seekBar.setMax(Video.getDuration());

                if (playSt == PlayState.STOP) {
                    seekBar.setProgress(Video.getDuration());
                }
                else {
                    seekBar.setProgress(Video.getCurrentPosition());
                }

                if (playSt == PlayState.PLAYING) {
                    // 再生中 指定間隔でシークバーを更新
                    SeekVerTimer(100);
                }
            }

            seekBar.setVisibility(View.VISIBLE);
        }
        else {
            seekBar.setVisibility(View.INVISIBLE);
        }
    }

    // シークバー制御タイマー開始
    private void SeekVerTimer(long microSecond)
    {
        SeekVerTimerStop();

        SeekVerTimer = new Timer();
        SeekVerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SeekVerHandler.post(new Runnable() {
                    public void run() {
                        SeekVerUpdate(NowScreen, PlayState.PLAYING);
                    }
                });
            }
        }, microSecond);
    }

    // シーク関連タイマー停止
    private void SeekVerTimerStop()
    {
        if(null != SeekVerTimer) {
            SeekVerTimer.cancel();
            SeekVerTimer = null;
        }
    }

    // ファイルダウンロード関連タイマー開始
    private void LoadingTimer(long MicroSecond)
    {
        if(null != DownLoadCheckTimer) {
            DownLoadCheckTimer.cancel();
            DownLoadCheckTimer = null;
        }

        DownLoadCheckTimer = new Timer();
        DownLoadCheckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DownLoadCheckHandler.post(new Runnable() {
                    public void run() {
                        LoadingTimerStop();

                        // ロード状況取得
                        int percent = Afd.GetLoadedBytePercent();

                        if(100 <= percent) {
                            funcDownloadComplete(NowUrl);
                        }
                        else if(0 > percent) {
                            Stop();
                        }
                        else {
                            // ロード中:プログレスバー更新
                            SeekVerUpdate(NowScreen, PlayState.DOWNLOADING);

                            // タイマーリスタート
                            LoadingTimer(100);
                        }
                    }
                });
            }
        }, MicroSecond);
    }

    // ファイルダウンロード関連タイマー停止
    private void LoadingTimerStop()
    {
        if(null != DownLoadCheckTimer) {
            DownLoadCheckTimer.cancel();
            DownLoadCheckTimer = null;
        }
    }
}