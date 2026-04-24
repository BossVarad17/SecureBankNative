package com.securebank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.securebank.app.R;
import com.securebank.app.network.ApiClient;
import com.securebank.app.utils.SessionManager;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvSignup    = findViewById(R.id.tvSignup);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty())    { etEmail.setError("Email is required");       return; }
        if (password.isEmpty()) { etPassword.setError("Password is required"); return; }

        setLoading(true);

        ApiClient.getApiService().login(email, password)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        setLoading(false);
                        try {
                            // Always read the raw string first — works for both 2xx and 4xx
                            ResponseBody rb = response.isSuccessful()
                                    ? response.body() : response.errorBody();
                            if (rb == null) {
                                Toast.makeText(LoginActivity.this,
                                        "Empty response from server", Toast.LENGTH_LONG).show();
                                return;
                            }
                            String raw = rb.string();
                            Log.d(TAG, "HTTP " + response.code() + " → " + raw);

                            JSONObject json = new JSONObject(raw);
                            String status  = json.optString("status", "error");
                            String message = json.optString("message", "Login failed");

                            if ("success".equals(status)) {
                                int userId       = json.getInt("user_id");
                                String userName  = json.getString("user_name");
                                String userEmail = json.getString("user_email");
                                new SessionManager(LoginActivity.this)
                                        .saveSession(userId, userName, userEmail);
                                Intent intent = new Intent(LoginActivity.this,
                                        DashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        message, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Response parse error", e);
                            Toast.makeText(LoginActivity.this,
                                    "Server may be starting up. Please wait and try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        setLoading(false);
                        Log.e(TAG, "Network failure", t);
                        Toast.makeText(LoginActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : "Sign In");
    }
}
