package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String SERVER_URL = "http://10.0.2.2:11771/send_message";

    private DatabaseHelper dbHelper;
    private ListView chatListView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private String currentTableName;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatListView = findViewById(R.id.chat_list_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messages);
        chatListView.setAdapter(messageAdapter);

        String username = getIntent().getStringExtra("username");
        dbHelper = new DatabaseHelper(this, username + ".db");
        client = new OkHttpClient();

        // 获取最新的session表名
        currentTableName = getLatestSessionTable();
        if (currentTableName == null) {
            // 如果没有找到任何会话表，创建一个新的会话表
            currentTableName = "session_" + getCurrentFormattedDate();
            dbHelper.createSessionTable(currentTableName);
        }

        // 加载历史消息
        loadHistory();

        messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String messageContent = messageEditText.getText().toString();
                    if (!messageContent.isEmpty()) {
                        sendMessage(messageContent);
                        messageEditText.setText("");
                    }
                    return true; // 处理了发送动作
                } else if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        if (event.isShiftPressed()) {
                            // 如果同时按下Shift键和回车键，允许换行
                            messageEditText.append("\n");
                            return true; // 消费了事件，允许换行
                        } else {
                            // 只按下回车键，发送消息
                            String messageContent = messageEditText.getText().toString().replace("\n", "").trim();
                            if (!messageContent.isEmpty()) {
                                sendMessage(messageContent);
                                messageEditText.setText("");
                            }
                            return true; // 消费了事件，进行发送
                        }
                    }
                }
                return false; // 没有消费事件
            }
        });

        sendButton.setOnClickListener(v -> {
            String messageContent = messageEditText.getText().toString();
            if (!messageContent.isEmpty()) {
                sendMessage(messageContent);
                messageEditText.setText("");
            }
        });
    }

    // 获取当前格式化后的日期
    private String getCurrentFormattedDate() {
        try {
            // 获取当前时间戳
            long currentTimeMillis = System.currentTimeMillis();
            // 创建Date对象
            Date date = new Date(currentTimeMillis);
            // 创建SimpleDateFormat对象
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.US);
            String formattedDate = sdf.format(date);
            // 格式化日期
            Log.d(TAG,"格式化日期时间: " + formattedDate);
            return formattedDate;
        }catch (Exception e){
            Log.d(TAG, "错误日期", e);
            return null;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_history) {
            showHistoryDialog();
            return true;
        } else if (itemId == R.id.action_new_session) {
            startNewConversation();
            return true;
        } else if (itemId == R.id.action_delete) {
            confirmDeleteCurrentSession();
            return true;
        } else if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showHistoryDialog() {
        Cursor cursor = dbHelper.getAllSessionTables();
        List<String> sessionTables = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                if (tableName.startsWith("session_")) {
                    sessionTables.add(tableName);
                }
            }
            cursor.close();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a session")
                .setItems(sessionTables.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentTableName = sessionTables.get(which);
                        loadHistory();
                    }
                });
        builder.create().show();
    }

    private void startNewConversation() {
        // 删除所有没有消息的会话表
        deleteEmptySessions();

        currentTableName = "session_" + getCurrentFormattedDate();
        dbHelper.createSessionTable(currentTableName);
        messages.clear();
        messageAdapter.notifyDataSetChanged();
    }

    private void confirmDeleteCurrentSession() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Session")
                .setMessage("Are you sure you want to delete this session?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCurrentSession();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private String getLatestSessionTable() {
        Cursor cursor = dbHelper.getAllSessionTables();
        String latestTable = null;
        long latestTimestamp = 0;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                String[] parts = tableName.split("_");
                if (parts.length == 2) {
                    try {
                        long timestamp = Long.parseLong(parts[1]);
                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp;
                            latestTable = tableName;
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid table name format: " + tableName);
                    }
                }
            }
            cursor.close();
        }
        return latestTable;
    }

    private void sendMessage(String messageContent) {
        // 插入用户消息到数据库并获取消息ID
        long messageId = dbHelper.insertUserMessage(currentTableName, messageContent);
        Message userMessage = new Message(messageContent, true);
        updateChat(userMessage);

        // 创建JSON对象封装用户消息
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", messageContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 创建请求体
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonObject.toString()
        );

        // 创建请求
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error sending message", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String jsonResponse = response.body().string();
                    Log.d(TAG, "Server response: " + jsonResponse);
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    if (jsonObject.has("msg")) {
                        String serverMessageContent = jsonObject.getString("msg");

                        // 更新数据库中的服务器消息
                        dbHelper.updateServerMessage(currentTableName, messageId, serverMessageContent);

                        // 更新UI
                        Message serverMessage = new Message(serverMessageContent, false);
                        updateChat(serverMessage);
                    } else {
                        Log.e(TAG, "No 'msg' key in JSON response");
                        runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "JSON parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateChat(Message message) {
        runOnUiThread(() -> {
            messages.add(message);
            messageAdapter.notifyDataSetChanged();
            chatListView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    // 读取表中的所有消息
    private void loadHistory() {
        messages.clear();  // 清除当前消息
        Cursor cursor = dbHelper.getAllMessages(currentTableName);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String userMessage = cursor.getString(cursor.getColumnIndex("user_message"));
                @SuppressLint("Range") String serverMessage = cursor.getString(cursor.getColumnIndex("server_message"));

                if (userMessage != null) {
                    messages.add(new Message(userMessage, true));
                }
                if (serverMessage != null) {
                    messages.add(new Message(serverMessage, false));
                }
            }
            cursor.close();
            messageAdapter.notifyDataSetChanged();
        }
    }

    // 删除当前会话表
    private void deleteCurrentSession() {
        dbHelper.deleteTable(currentTableName);
        String latestTableName = getLatestSessionTable();

        if (latestTableName != null) {
            currentTableName = latestTableName;
            loadHistory();
        } else {
            // 如果没有找到任何会话表，创建一个新的会话表
            currentTableName = "session_" + getCurrentFormattedDate();
            dbHelper.createSessionTable(currentTableName);
            messages.clear();
            messageAdapter.notifyDataSetChanged();
        }
    }

    // 删除所有没有消息的会话表
    private void deleteEmptySessions() {
        Cursor cursor = dbHelper.getAllSessionTables();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                if (tableName.startsWith("session_")) {
                    Cursor messageCursor = dbHelper.getAllMessages(tableName);
                    if (messageCursor != null && messageCursor.getCount() == 0) {
                        dbHelper.deleteTable(tableName);
                    }
                    if (messageCursor != null) {
                        messageCursor.close();
                    }
                }
            }
            cursor.close();
        }
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
