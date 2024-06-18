package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;

    public DatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 初次创建数据库时调用
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库版本升级时调用
    }

    // 创建session表
    public void createSessionTable(String tableName) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_message TEXT, " +
                "server_message TEXT)";
        db.execSQL(createTable);
    }

    // 插入新消息ID并写入用户输入
    public long insertUserMessage(String tableName, String userMessage) {
        ContentValues values = new ContentValues();
        values.put("user_message", userMessage);
        return db.insert(tableName, null, values);
    }

    // 根据消息ID写入服务器消息
    public void updateServerMessage(String tableName, long messageId, String serverMessage) {
        ContentValues values = new ContentValues();
        values.put("server_message", serverMessage);
        db.update(tableName, values, "id = ?", new String[]{String.valueOf(messageId)});
    }

    // 删除数据表
    public void deleteTable(String tableName) {
        String dropTable = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(dropTable);
    }

    // 读取表中所有消息
    public Cursor getAllMessages(String tableName) {
        String query = "SELECT * FROM " + tableName;
        return db.rawQuery(query, null);
    }

    // 获取所有session表名
    public Cursor getAllSessionTables() {
        String query = "SELECT name FROM sqlite_master WHERE type='table'";
        return db.rawQuery(query, null);
    }
}
