package com.example.nakayama.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.File;

import com.example.nakayama.myapplication.Types.MovieDataClass;
import com.example.nakayama.myapplication.Types.ScreenState;
import com.example.nakayama.myapplication.Types.PlayState;

public class MainActivity extends Activity implements MovieListener
{
    // Video制御 オブジェクト
    private MovieControl Video = null;

    // リスト選択データ
    Map<String, String> listSelectingMap;

    // 再生中のURL
    private String UrlBack = null;

    // 最終表示画面情報
    static ScreenState ScreenStBack = ScreenState.FULL ;

    // 現在の画面情報
    private static ScreenState nScreenState = ScreenState.MINI;

    // 初期再生リスト
    private MovieDataClass[] InitMdcList = new MovieDataClass[] {
            new MovieDataClass(1, "ダチョウ",        "http://www.ajisaba.net/motion/dnld.php?fpath=emu.mp4", null),
            new MovieDataClass(2, "ペンギン",        "http://www.ajisaba.net/motion/dnld.php?fpath=penguin.mp4", null),
            new MovieDataClass(3, "ゴリラ",           "http://www.next-pit.net/download.php?mode=dl&file=20130705_MST_1280x720(4.8M).mp4", null),
            new MovieDataClass(4, "NHK MOVIE 1",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100040_00000_V_000.mp4", null),
            new MovieDataClass(5, "NHK MOVIE 2",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100046_00000_V_000.mp4", null),
            new MovieDataClass(6, "NHK MOVIE 3",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100042_00000_V_000.mp4", null),
            new MovieDataClass(7, "NHK MOVIE 4",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100043_00000_V_000.mp4", null),
            new MovieDataClass(8, "NHK MOVIE 5",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100044_00000_V_000.mp4", null),
            new MovieDataClass(9, "NHK MOVIE 6",    "http://www9.nhk.or.jp/das/movie/D0002100/D0002100047_00000_V_000.mp4", null),

    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView head = (TextView)findViewById(R.id.header);
        head.setText("Play list");

        nScreenState = ScreenState.MINI;

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
        ListViewAdapter adapter = new ListViewAdapter(this, movies,
                R.layout.listview_item,
                new String[]{"TITLE","URL"},
                new int[]{R.id.listMain_text, R.id.listSub_text},
                R.id.listMenu_button,
                R.id.Item_Liner
        );

        ListView listView = (ListView) findViewById(R.id.listView);
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
                        inflater.inflate(R.menu.activity_delete, popup.getMenu());
                        popup.show();

                        // ポップアップメニューのメニュー項目のクリック処理
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == R.id.deleteItem) {

                                    // 確認ダイアログの生成
                                    AlertDialog.Builder alertDlg = new AlertDialog.Builder(getWindow().getContext());
                                    alertDlg.setTitle("確認");
                                    alertDlg.setMessage("項目を削除してよろしいですか？");
                                    alertDlg.setPositiveButton(
                                            "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // OK ボタンクリック処理
                                                    funcDeleteClick(listSelectingMap.get("URL"));
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

        // ビデオ制御初期化
        Video = new MovieControl(getWindow().getDecorView());
        Video.SetMovieListener(this);

        // 追加ボタン押下イベント設定
        Button addButton = (Button) findViewById(R.id.add_button);
        if (null != addButton) {
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    funcAddButtonClick(v);
                }
            });
        }

        // リスト初期化ボタン押下イベント設定
        Button initButton = (Button) findViewById(R.id.initlist_button);
        if (null != initButton) {
            initButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 確認ダイアログの生成
                    AlertDialog.Builder alertDlg = new AlertDialog.Builder(getWindow().getContext());
                    alertDlg.setTitle("確認");
                    alertDlg.setMessage("項目を初期化してよろしいですか？");
                    alertDlg.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK ボタンクリック処理
                                    funcInitListButtonClick(null);
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

        // スクリーン更新
        ScreenUpdate(ScreenState.MINI);
    }

    @Override
    // [イベント] 各画面から戻った時の処理
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(0 == resultCode) {
            // リスト更新
            ListUpdate();
        }
    }

    // [イベント]リストビュークリック処理
    private void funcListViewClick(String url)
    {
        Video.PlayStart(url, GetLocalMoviePath(url), 0);

        UrlBack = url;

        // 現在のスクリーン状態を更新
        ScreenUpdate(ScreenState.MINI);
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

    @Override
    // [イベント]動画スライド処理
    public void onMovieSlide()
    {
        if (nScreenState == ScreenState.FULL) {
            nScreenState = ScreenState.MINI;

            ScreenUpdate(ScreenState.MINI);

            Log.d("MainActivity", "mini window change");
        }
        else {

            // ミニ画面表示クリア
            Video.MiniViewClear();

            Log.d("MainActivity", "mini window invisible");
        }
    }

    @Override
    // [イベント]動画クリック処理
    public void onMovieTouch()
    {
        Intent intent = new Intent(
                getApplication(), PlayMovieActivity.class);

        Video.PlayStop();

        if(UrlBack != null)
        {
            intent.putExtra("URL", UrlBack);
            intent.putExtra("POS", Video.GetView().getCurrentPosition());

        }

        startActivityForResult(intent, 0);

        Video.GetView().setVisibility(View.INVISIBLE);
    }

    @Override
    // [イベント]ダウンロード完了
    public void onDownloadComplete(String url)
    {
        // ダウンロード処理無し
    }

    @Override
    // [イベント]スクリーン切り替えボタンクリック
    public void onScreenButton()
    {
        // 発生無し
    }

    // [イベント]ローカルファイル削除ボタンクリック
    private void funcDeleteClick(String url)
    {
        String path = GetLocalMoviePath(url);

        if(path != null) {
            // ローカルファイルが残っていたら削除
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }

        // データベースから削除
        DeleteMovie(url);

        // リスト更新
        ListUpdate();
    }

    // [イベント]閉じるボタンクリック
    private void funcFinishButtonClick(View v)
    {
        Video.Close();

        finish();
    }

    // [イベント]動画追加ボタンクリック
    private void funcAddButtonClick(View v)
    {
        Intent intent = new Intent(
                getApplication(), ItemAddActivity.class);

        startActivityForResult(intent, 0);
    }

    // [イベント]リストの初期化ボタンクリック
    private void funcInitListButtonClick(View v)
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl";
        movies.addAll(sql.Select(query));

        // 削除処理
        for(MovieDataClass mdc : movies) {

            String path = GetLocalMoviePath(mdc.url);

            if(path != null) {
                // ローカルファイルが残っていたら削除
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }

            // SQLから削除
            sql.Delete(movies.get(0));
        }

        // 初期値登録
        for(MovieDataClass mdc : InitMdcList) {
            sql.Insert(mdc);
        }

        ListUpdate();

        Toast.makeText(this, "初期化しました", Toast.LENGTH_SHORT).show();
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
        ListViewAdapter adapter = new ListViewAdapter(this, movies,
                R.layout.listview_item,
                new String[]{"TITLE", "URL"},
                new int[]{R.id.listMain_text, R.id.listSub_text},
                R.id.listMenu_button,
                R.id.Item_Liner
        );

        ListView listView = (ListView) findViewById(R.id.listView);
        if (null != listView) {
            listView.setAdapter(adapter);
        }
    }

    // レイアウトの更新
    private void ScreenUpdate(ScreenState screenSt)
    {
        LinearLayout liner =
                (LinearLayout) findViewById(R.id.videoLayout);

        FrameLayout frame =
                (FrameLayout) findViewById(R.id.FrameLayout);

        // VideoView表示
        Video.GetView().setVisibility(View.VISIBLE);

        // 前回のステータスと違うときに更新
        if(ScreenState.MINI != ScreenStBack) {
            // 画面縦固定
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            // リストを表示
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setVisibility(View.VISIBLE);

            // 通知領域の表示(フラグクリア)
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // 背景を透明
            liner.setBackgroundColor(Color.argb(0, 0, 0, 0));

            // LinearLayoutの右下に表示
            liner.setGravity(Gravity.BOTTOM | Gravity.RIGHT);

            // VideoView のサイズを縮小
            LinearLayout.LayoutParams layoutParam =
                    new LinearLayout.LayoutParams(400, 280);

            // サイズをVideoViewを含むフレームに反映
            frame.setLayoutParams(layoutParam);

            if(UrlBack == null)
            {
                Video.GetView().setVisibility(View.INVISIBLE);
            }
        }

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

    // 動画の削除
    private void DeleteMovie(String url)
    {
        Database sql = new Database(getBaseContext());
        ArrayList<MovieDataClass> movies =
                new ArrayList<MovieDataClass>(){};

        // DB取得
        String query = "SELECT * FROM newtbl WHERE url='" + url + "'";
        movies.addAll(sql.Select(query));

        if(movies.size() > 0) {
            sql.Delete(movies.get(0));

            Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
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
            btn.setTag(position);

            final ListView list =  (ListView) parent;

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

            return view;
        }
    }


}
