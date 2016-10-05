package com.example.nakayama.myapplication;

/**
 * Created by Nakayama on 2016/09/09.
 */
public class Types {
    // リスト表示クラス
    public static class MovieDataClass
    {
        public int no;
        public String title;
        public String url;
        public String local;

        public MovieDataClass(int N, String T, String U, String L)
        {
            no = N;
            title = T;
            url = U;
            local = L;
        }
    }

    // 再生状態
    public  enum PlayState {
        NOT_CHANGE,
        STOP,
        PAUSE,
        LOADING,
        PLAYING,
        DOWNLOADING
    }

    // 動画View表示状態
    public  enum ScreenState {
        FULL,
        MINI,
        TOP
    }
}
