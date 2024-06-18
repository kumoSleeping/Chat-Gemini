package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        client = new OkHttpClient();

        // 限制用户名输入不支持换行
        usernameEditText.setSingleLine(true);

        loginButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            login(username, password);
        });

        registerButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            register(username, password);
        });
    }

    private void login(final String username, String password) {
        String url = "http://10.0.2.2:11771/login?username=" + username + "&password=" + password;
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("LoginActivity", "Error during login request", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Login failed: " + response.code() + " " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                if ("success".equals(responseBody)) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + responseBody, Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Login failed: " + responseBody);
                }
            }
        });
    }

    private void register(final String username, String password) {
        // 验证用户名是否合法
        if (username.isEmpty() || !isValidUsername(username)) {
            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid username. Only alphanumeric characters are allowed.", Toast.LENGTH_SHORT).show());
            return;
        }

        String url = "http://10.0.2.2:11771/register";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", username);
            jsonObject.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonObject.toString()
        );
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("LoginActivity", "Error during register request", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Register failed", Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Register failed: " + response.code() + " " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                if ("success".equals(responseBody)) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Register successful", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Register failed: " + responseBody, Toast.LENGTH_SHORT).show());
                    Log.e("LoginActivity", "Register failed: " + responseBody);
                }
            }
        });
    }

    // 验证用户名是否有效，仅允许字母和数字
    private boolean isValidUsername(String username) {
        String regex = "^[a-zA-Z0-9]+$";
        return Pattern.matches(regex, username);
    }
}
