package com.example.nakayama.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ItemAddActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itemadd);

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

        // キャンセルボタン押下イベント設定
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        if (null != cancelButton) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    funcCancelButtonClick(v);
                }
            });
        }
    }

    // [イベント]動画追加ボタンクリック
    private void funcAddButtonClick(View v)
    {
        EditText editTitle = (EditText) findViewById(R.id.editTitle);
        EditText editUrl = (EditText) findViewById(R.id.editUrl);

        String title = editTitle.getText().toString();
        String url = editUrl.getText().toString();

        // 入力チェック
        if(0 < title.length() && 0 < url.length()) {
            Database sql = new Database(getBaseContext());

            // 連番取得
            int no = sql.MaxNo() + 1;

            // 登録データ生成
            Types.MovieDataClass data =
                    new Types.MovieDataClass(no, title, url, null);

            sql.Insert(data);

            Toast.makeText(this, "登録しました", Toast.LENGTH_SHORT).show();

            finish();
        }
        else {
            Toast.makeText(this, "入力に誤りがあります", Toast.LENGTH_SHORT).show();
        }
    }

    // [イベント]キャンセルボタンクリック
    private void funcCancelButtonClick(View v)
    {
        finish();
    }
}
