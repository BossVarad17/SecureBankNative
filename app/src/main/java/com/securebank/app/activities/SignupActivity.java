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

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnSignup;
    private ProgressBar progressBar;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullName        = findViewById(R.id.etFullName);
        etEmail           = findViewById(R.id.etEmail);
        etPhone           = findViewById(R.id.etPhone);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup         = findViewById(R.id.btnSignup);
        progressBar       = findViewById(R.id.progressBar);
        tvLogin           = findViewById(R.id.tvLogin);

        btnSignup.setOnClickListener(v -> attemptSignup());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
        String fullName        = etFullName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String phone           = etPhone.getText().toString().trim();
        String password        = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (fullName.isEmpty())  { etFullName.setError("Name is required");         return; }
        if (email.isEmpty())     { etEmail.setError("Email is required");            return; }
        if (password.isEmpty())  { etPassword.setError("Password is required");      return; }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match"); return;
        }

        setLoading(true);

        ApiClient.getApiService().signup(fullName, email, phone, password, confirmPassword)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        setLoading(false);
                        try {
                            ResponseBody rb = response.isSuccessful()
                                    ? response.body() : response.errorBody();
                            if (rb == null) {
                                Toast.makeText(SignupActivity.this,
                                        "Empty response from server", Toast.LENGTH_LONG).show();
                                return;
                            }
                            String raw = rb.string();
                            Log.d(TAG, "HTTP " + response.code() + " → " + raw);

                            JSONObject json = new JSONObject(raw);
                            String status  = json.optString("status", "error");
                            String message = json.optString("message", "Signup failed");

                            if ("success".equals(status)) {
                                int userId      = json.getInt("user_id");
                                String userName = json.getString("user_name");
                                String acctNum  = json.getString("account_number");
                                new SessionManager(SignupActivity.this)
                                        .saveSession(userId, userName, email);
                                Toast.makeText(SignupActivity.this,
                                        "Welcome! Account No: " + acctNum,
                                        Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(SignupActivity.this,
                                        DashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(SignupActivity.this,
                                        message, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Response parse error", e);
                            Toast.makeText(SignupActivity.this,
                                    "Server may be starting up. Please wait and try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        setLoading(false);
                        Log.e(TAG, "Network failure", t);
                        Toast.makeText(SignupActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
        btnSignup.setText(loading ? "Creating account…" : "Create Account");
    }
}
