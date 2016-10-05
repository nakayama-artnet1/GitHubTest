package com.example.nakayama.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.nakayama.myapplication.Types.MovieDataClass;

import java.util.ArrayList;

/**
 * Created by Nakayama on 2016/09/09.
 */

public class Database
{
    SQLiteOpenHelper Sql;
    public Database(Context context)
    {
        Sql = new MySQLiteOpenHelper(context);
    }

    // 生成
    public void Create(Context context)
    {
        //return new Database(context);
    }

    // 挿入
    public boolean Insert(MovieDataClass data)
    {
        boolean ret = false;
        ContentValues values = new ContentValues();
        values.put("no", data.no);
        values.put("title", data.title);
        values.put("url", data.url);
        values.put("local", data.local);

        SQLiteDatabase db = Sql.getWritableDatabase();

        try {
            db.insert("newtbl", null, values);
            ret = true;
        } finally {
            db.close();
        }
        return ret;
    }

    // 更新
    public boolean Update(MovieDataClass data)
    {
        ContentValues values = new ContentValues();
        values.put("title", data.title);
        values.put("url", data.url);
        values.put("local", data.local);

        String whereClause = "no = ?";
        String whereArgs[] = new String[1];
        whereArgs[0] = String.valueOf(data.no);

        SQLiteDatabase db = Sql.getWritableDatabase();

        try {
            db.update("newtbl", values, whereClause, whereArgs);
        }
        finally {
            db.close();
        }
        return true;
    }

    // 削除
    public boolean Delete(MovieDataClass data)
    {
        String whereClause = "no = ?";
        String whereArgs[] = new String[1];
        whereArgs[0] = String.valueOf(data.no);

        SQLiteDatabase db = Sql.getWritableDatabase();

        try {
            db.delete("newtbl", whereClause, whereArgs);
        }
        finally {
            db.close();
        }

        return true;
    }

    // 検索
    public ArrayList<MovieDataClass> Select(String query)
    {
        int i = 0;

        SQLiteDatabase db = Sql.getWritableDatabase();

        ArrayList<MovieDataClass> movies = new ArrayList<MovieDataClass>();

        try {

            //SQL文の実行
            Cursor cursor = db.rawQuery(query , null);

            while (cursor.moveToNext()){
                int no = cursor.getInt(0);
                String title = cursor.getString(1);
                String url = cursor.getString(2);
                String local = cursor.getString(3);

                movies.add(new MovieDataClass(no,
                        title,
                        url,
                        local));
                i ++;
            }
        }
        finally {
            db.close();
        }

        return movies;
    }

    // 連番最大値取得
    public int MaxNo()
    {
        SQLiteDatabase db = Sql.getWritableDatabase();

        int no = 0;

        try {
            //SQL文の実行
            Cursor cursor = db.rawQuery("SELECT MAX(no) FROM newtbl", null);
            if(cursor.moveToNext()) {
                no = cursor.getInt(0);
            }
        }
        finally {
            db.close();
        }

        return no;
    }

    public class MySQLiteOpenHelper extends SQLiteOpenHelper {

        public MySQLiteOpenHelper(Context context) {
            // 任意のデータベースファイル名と、バージョンを指定する
            super(context, "newtbl.db", null, 1);
        }

        /**
         * このデータベースを初めて使用する時に実行される処理
         * @param db
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            // テーブルの作成、初期データの投入等を行う。
            db.execSQL("create table newtbl ("
                        + "no integer primary key autoincrement not null, "
                        + "title text not null, "
                        + "url text not null, "
                        + "local text )" );
        }

        /**
         * アプリケーションの更新などによって、データベースのバージョンが上がった場合に実行される処理
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // データの退避、テーブルの再構成等を行う。
        }
    }

    public class SqlCursorClass {
        ArrayList<String> columns;
    }
}



