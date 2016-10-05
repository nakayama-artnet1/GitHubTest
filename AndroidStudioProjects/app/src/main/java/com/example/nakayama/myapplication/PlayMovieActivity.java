package com.example.nakayama.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import com.example.nakayama.myapplication.Types.MovieDataClass;
import com.example.nakayama.myapplication.Types.ScreenState;
import com.example.nakayama.myapplication.Types.PlayState;

public class PlayMovieActivity extends Activity implements MovieListener
{
    // VideoView オブジェクト
    private MovieControl Video = null;

    // リスト選択データ
    Map<String, String> listSelectingMap;

    // 再生中のURL
    private String UrlBack;

    // 最終表示画面情報
    static ScreenState ScreenStBack;

    // 現在の画面情報
    private static ScreenState nScreenState;

    // ダウンロードリスト
    ArrayList<DownloadListClass> DownLoads = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_movie);

        TextView head = (TextView)findViewById(R.id.header);
        head.setText("Viewer");

        ScreenStBack = ScreenState.FULL;
        nScreenState = ScreenState.TOP;

        Database sql = new Database(getBaseContext());

        // データベースからリスト取得
        ArrayList<MovieDataClass> MdcList =
                new ArrayList<MovieDataClass>(){};

        String query = "SELECT * FROM newtbl";
        MdcList.addAll(sql.Select(query));

        // リスト初期化
        List<Map<String, String>> movies = new ArrayList<Map<String, String>>();
        for (MovieDataClass mdc : MdcList) {
            Map<String, String> conMap = new HashMap<String, String>();
            conMap.put("TITLE", mdc.title);
            conMap.put("URL", mdc.url);
            movies.add(conMap);
        }

        // リスト用アダプタ
        final ListViewAdapter adapter = new ListViewAdapter(this, movies,
                R.layout.listview_item,
                new String[]{"TITLE","URL"},
                new int[]{R.id.listMain_text, R.id.listSub_text},
                R.id.listMenu_button,
                R.id.Item_Liner
        );

        ListView listView = (ListView) findViewById(R.id.listView2);
        if (null != listView) {
            listView.setAdapter(adapter);

            // リスト選択イベント設定
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    ListView listView = (ListView) parent;
                    listSelectingMap =
                            (Map<String, String>) listView.getItemAtPosition(position);

                    if(0 == (id & 0x8000)) {
                        // オプションメニュー表示
                        PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                        MenuInflater inflater = popup.getMenuInflater();
                        if(null != GetLocalMoviePath(listSelectingMap.get("URL"))) {
                            // ダウンロード済み
                            inflater.inflate(R.menu.activity_localfile_menu, popup.getMenu());
                        }
                        else {
                            // 未ダウンロード
                            inflater.inflate(R.menu.activity_streaming_menu, popup.getMenu());
                        }
                        popup.show();

                        // ポップアップメニューのメニュー項目のクリック処理
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == R.id.playItem) {
                                    funcListViewClick(listSelectingMap.get("URL"));
                                }
                                else if(item.getItemId() == R.id.downloadItem){
                                    funcDownloadClick(listSelectingMap.get("URL"));
                                }
                                else {
                                    funcDeleteClick(listSelectingMap.get("URL"));
                                }
                                return true;
                            }
                        });
                    }
                    else {
                        // ボタン以外のタップは再生
                        funcListViewClick(listSelectingMap.get("URL"));
                    }
                }
            });
        }

        // VideoView初期化
        Video = new MovieControl(getWindow().getDecorView());
        Video.SetMovieListener(this);

        // 終了ボタン押下イベント設定
        Button endButton = (Button) findViewById(R.id.end_button2);
        if (null != endButton) {
            endButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    funcFinishButtonClick(v);
                }
            });
        }

        // 戻るボタン押下イベント設定
        Button h_endButton = (Button) findViewById(R.id.h_modoru);
        if (null != h_endButton) {
            h_endButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    funcFinishButtonClick(v);
                }
            });
        }

        // ALLダウンロードボタン押下イベント設定
        Button allButton = (Button) findViewById(R.id.alldownload_button);
        if (null != allButton) {
            allButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    funcAllDownloadButtonClick();
                }
            });
        }

        // ALLファイル削除ボタン押下イベント設定
        Button AllDelButton = (Button) findViewById(R.id.alldel_button);
        if (null != AllDelButton) {
            AllDelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 確認ダイアログの生成
                    AlertDialog.Builder alertDlg = new AlertDialog.Builder(getWindow().getContext());
                    alertDlg.setTitle("確認");
                    alertDlg.setMessage("ローカルファイルを全て削除します");
                    alertDlg.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK ボタンクリック処理
                                    funcAllDeleteButtonClick();
                                }
                            });
                    alertDlg.setNegativeButton(
                            "Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Cancel ボタンクリック処理
                                }
                            });

                    // 表示
                    alertDlg.create().show();
                }
            });
        }

        // 画面初期設定
        ScreenUpdate(nScreenState);

        Intent i = getIntent();
        String prevUrl = i.getStringExtra("URL");
        int prevPos = i.getIntExtra("POS", -1);
        if(prevUrl != null && prevPos != -1) {
            Video.PlayStart(
                    prevUrl, GetLocalMoviePath(prevUrl), prevPos);
        }
    }

    // [イベント処理]リストビュークリック処理
    private void funcListViewClick(String url)
    {
        UrlBack = url;

        // ダウンロード中ならキャンセル
        Video.FileDownloadCancel();

        // タイトルに紐づくURLを再生
        Video.PlayStart(url, GetLocalMoviePath(url), 0);
    }

    @Override
    // [イベント]動画スライド処理
    public void onMovieSlide()
    {
        // 処理無し
    }

    @Override
    // [イベント]動画クリック処理
    public void onMovieTouch()
    {
        // 制御ボタン再表示の為スクリーン更新
        ScreenUpdate(nScreenState);
    }

    @Override
    // [イベント]次へボタンクリック処理
    public void onPlayNext()
    {
        // 次の動画再生
        funcListViewClick(
                GetNextPlayMovie(UrlBack));
    }

    @Override
    // [イベント]戻るボタンクリック処理
    public void onPlayPrev()
    {
        // 前の動画再生
        funcListViewClick(
                GetPrevPlayMovie(UrlBack));
    }

    // [イベント] ダウンロードボタンクリック
    private void funcDownloadClick(String url)
    {
        // ファイルダウンロード開始
        Video.FileDownload(
                this, GetTitle(url), url, CreateLocalMoviePath(url));

        UrlBack = url;

        ScreenUpdate(nScreenState);
    }

    // [イベント] 全ダウンロードボタンクリック
    private void funcAllDownloadButtonClick()
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>();
        // DB取得
        String query = "SELECT * FROM newtbl WHERE local is null";
        movies.addAll(sql.Select(query));

        DownLoads = new ArrayList<DownloadListClass>();

        // ダウンロードリスト生成
        for(MovieDataClass mdc: movies) {
            DownLoads.add(
                    new DownloadListClass(mdc.url, mdc.local));
        }

        if(DownLoads.size() > 0) {
            // ファイルダウンロード開始(初回)
            String url = DownLoads.get(0).url;
            Video.FileDownload(
                    this, GetTitle(url), url, CreateLocalMoviePath(url));

            UrlBack = url;
        }
    }

    // [イベント] 全ファイル削除ボタンクリック
    private void funcAllDeleteButtonClick()
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>();
        // DB取得
        String query = "SELECT * FROM newtbl WHERE local not null";
        movies.addAll(sql.Select(query));

        DownLoads = new ArrayList<DownloadListClass>();

        // ローカルファイル削除
        for(MovieDataClass mdc: movies) {
            funcDeleteClick(mdc.url);
        }
    }

    @Override
    // [イベント]ダウンロード完了
    public void onDownloadComplete(String url)
    {
        SetLocalMoviePath(url);

        // リスト更新
        ListUpdate();

        if(DownLoads == null) {
            // 直後に再生開始
            funcListViewClick(url);
        }
        else {
            // ダウンロード完了ファイルのリスト削除
            DownLoads.remove(0);

            // 残りがあれば続ける
            if(DownLoads.size() > 0) {
                // ファイルダウンロード開始
                String nextUrl = DownLoads.get(0).url;
                Video.FileDownload(this,
                        GetTitle(nextUrl),
                        nextUrl,
                        CreateLocalMoviePath(nextUrl));

                UrlBack = nextUrl;
            }
            else {
                DownLoads.clear();
                DownLoads = null;
            }
        }
    }

    @Override
    // [イベント]スクリーン切り替えボタンクリック
    public void onScreenButton()
    {
        if (nScreenState == ScreenState.FULL) {
            nScreenState = ScreenState.TOP;
        }
        else {
            nScreenState = ScreenState.FULL;
        }
        ScreenUpdate(nScreenState);
    }

    // [イベント] ローカルファイル削除ボタンクリック
    private void funcDeleteClick(String url)
    {
        String path = GetLocalMoviePath(url);

        if(path == null) {
            return ;
        }

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        DeleteLocalMoviePath(url);

        // リスト更新
        ListUpdate();
    }

    // 閉じるボタンクリック
    private void funcFinishButtonClick(View v)
    {
        Video.Close();

        finish();
    }

    // 動画ビューの更新
    private void ScreenUpdate(ScreenState screenSt)
    {
        LinearLayout liner =
                (LinearLayout) findViewById(R.id.videoLayout);

        // VideoView表示
        Video.GetView().setVisibility(View.VISIBLE);

        if (ScreenState.FULL == screenSt) {

            // 前回のステータスと違うときに更新
            if(ScreenState.FULL != ScreenStBack) {

                // 画面横固定
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                // リスト領域を非表示
                LinearLayout listLinear = (LinearLayout) findViewById(R.id.list_linear);
                listLinear.setVisibility(View.INVISIBLE);

                //通知領域の非表示
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 背景黒
                liner.setBackgroundColor(Color.BLACK);

                // videoLayoutLayoutの中央に表示
                liner.setGravity(Gravity.CENTER);

                // VideoView のサイズを画面最大に拡大
                LinearLayout.LayoutParams layoutParam =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);

                // サイズをVideo Linerに反映
                liner.setLayoutParams(layoutParam);
            }
        }
        else {

            // 前回のステータスと違うときに更新
            if(ScreenState.TOP != ScreenStBack) {
                // 画面縦固定
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                // リストを表示
                LinearLayout listLinear = (LinearLayout) findViewById(R.id.list_linear);
                listLinear.setVisibility(View.VISIBLE);

                // 通知領域の表示(フラグクリア)
                getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 背景黒
                liner.setBackgroundColor(Color.BLACK);

                // LinearLayoutの上に表示
                liner.setGravity(Gravity.CENTER);

                // VideoView のサイズを標準に戻す
                LinearLayout.LayoutParams layoutParam =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 450);

                // サイズをVideo Linerに反映
                liner.setLayoutParams(layoutParam);
            }
        }

        // 動画更新
        Video.ViewUpdate(screenSt, PlayState.NOT_CHANGE);

        ScreenStBack = screenSt;
    }

    // 次の再生データ取得
    private String GetNextPlayMovie(String playNow)
    {
        String ret = "";
        boolean nextFlag = false;
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl";
        movies.addAll(sql.Select(query));

        for (MovieDataClass mdc : movies) {
            if(nextFlag) {
                // 現在の次のurl
                ret = mdc.url;
                break;
            }

            if(mdc.url.equals(playNow)) {
                // 現在再生中のURLが見つかった
                nextFlag = true;
            }
        }

        if(ret.length() <= 0) {
            // 見つからなかった場合先頭をセット
            ret = movies.get(0).url;
        }

        return ret;
    }

    // 前の再生データ取得
    private String GetPrevPlayMovie(String playNow)
    {
        String ret = "";
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl";
        movies.addAll(sql.Select(query));

        for (MovieDataClass mdc : movies) {
            if(mdc.url.equals(playNow)) {
                // 現在再生中のURLが見つかった
                break;
            }
            ret = mdc.url;
        }

        if(ret.length() <= 0) {
            // 先頭で見つかった場合終端をセット
            ret = movies.get(movies.size()-1).url;
        }

        return ret;
    }

    // URLからタイトルを取得
    private String GetTitle(String url)
    {
        String Ret = null;

        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            Ret = movies.get(0).title;
        }
        return Ret;
    }

    // ローカルパスの生成
    private String CreateLocalMoviePath(String url)
    {
        String Ret = null;
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            Ret = this.getFilesDir().getAbsolutePath()
                    + "/SampleFolder/"
                    + String.valueOf(movies.get(0).no)
                    + ".mp4";
        }
        return Ret;
    }

    // ローカルパスの保存
    private void SetLocalMoviePath(String url)
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            movies.get(0).local = CreateLocalMoviePath(url);

            // データベース更新
            sql.Update(movies.get(0));
        }
    }

    // ローカルパスの取得
    private String GetLocalMoviePath(String url)
    {
        String Ret = null;
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            Ret = movies.get(0).local;
        }

        return Ret;
    }

    // ローカルパスの削除
    private void DeleteLocalMoviePath(String url)
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            movies.get(0).local = null;

            sql.Update(movies.get(0));
        }

    }

    // リストの更新
    private void ListUpdate() {
        Database sql = new Database(getBaseContext());

        // データベースからリスト取得
        ArrayList<MovieDataClass> MdcList =
                new ArrayList<MovieDataClass>() {
                };

        String query = "SELECT * FROM newtbl";
        MdcList.addAll(sql.Select(query));

        // リスト初期化
        List<Map<String, String>> movies = new ArrayList<Map<String, String>>();
        for (MovieDataClass mdc : MdcList) {
            Map<String, String> conMap = new HashMap<String, String>();
            conMap.put("TITLE", mdc.title);
            conMap.put("URL", mdc.url);
            movies.add(conMap);
        }

        // リスト用アダプタ
        final ListViewAdapter adapter = new ListViewAdapter(this, movies,
                R.layout.listview_item,
                new String[]{"TITLE","URL"},
                new int[]{R.id.listMain_text, R.id.listSub_text},
                R.id.listMenu_button,
                R.id.Item_Liner
        );

        ListView listView = (ListView) findViewById(R.id.listView2);
        if (null != listView) {
            listView.setAdapter(adapter);
        }
    }

    // リストビュー用アダプタ
    public class ListViewAdapter extends SimpleAdapter {
        private int mButton;
        private int mLine;

        public ListViewAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to, int button, int line) {
            super(context, data, resource, from, to);
            mButton = button;
            mLine = line;
        }

        public View getView(final int position, View convertView, final ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            Button btn = (Button) view.findViewById(mButton);

            final ListView list =  (ListView) parent;

            btn.setTag(position);

            // ボタンを押下されたときのイベント設定
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg) {
                    AdapterView.OnItemClickListener listener = list.getOnItemClickListener();
                    if(null != listener) {
                        long id = getItemId(position);
                        listener.onItemClick((AdapterView<?>) parent, arg, position, id);
                    }
                }
            });

            LinearLayout ll = (LinearLayout) view.findViewById(mLine);
            ll.setTag(position);

            // ボタン以外を押下されたときのイベント設定
            ll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg) {
                    AdapterView.OnItemClickListener listener = list.getOnItemClickListener();
                    if(null != listener) {
                        long id = getItemId(position) | 0x8000;
                        listener.onItemClick((AdapterView<?>) parent, arg, position, id);
                    }
                }
            });

            // メニュー表示設定
            Map<String, String> maps =
                    (Map<String, String>) list.getItemAtPosition(position);

            if(null == GetLocalMoviePath(maps.get("URL"))) {
                btn.setText("↓");
            }
            else {
                btn.setText("□");
            }

            return view;
        }
    }

    // ダウンロードリスト用クラス
    public static class DownloadListClass
    {
        public String url;
        public String path;

        public DownloadListClass(String u, String p)
        {
            url = u;
            path = p;
        }
    }
}
