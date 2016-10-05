package com.example.nakayama.myapplication;

import java.util.EventListener;

/**
 * Created by Nakayama on 2016/09/12.
 */
public interface MovieListener extends EventListener
{
        void onMovieSlide();
        void onMovieTouch();
        void onPlayNext();
        void onPlayPrev();
        void onDownloadComplete(String url);
        void onScreenButton();
}
