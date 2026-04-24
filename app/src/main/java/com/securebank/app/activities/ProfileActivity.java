package com.securebank.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.securebank.app.R;
import com.securebank.app.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView tvName, tvEmail, tvPhone, tvMemberSince;
    private LinearLayout llProfileAccounts;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        tvName            = findViewById(R.id.tvName);
        tvEmail           = findViewById(R.id.tvEmail);
        tvPhone           = findViewById(R.id.tvPhone);
        tvMemberSince     = findViewById(R.id.tvMemberSince);
        llProfileAccounts = findViewById(R.id.llProfileAccounts);
        progressBar       = findViewById(R.id.progressBar);

        loadProfile();
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getProfile()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        progressBar.setVisibility(View.GONE);
                        try {
                            ResponseBody rb = response.isSuccessful()
                                    ? response.body() : response.errorBody();
                            if (rb == null) return;
                            JSONObject json = new JSONObject(rb.string());
                            if ("success".equals(json.optString("status"))) {
                                renderProfile(json);
                            } else {
                                Toast.makeText(ProfileActivity.this,
                                        json.optString("message", "Error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Profile parse error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderProfile(JSONObject json) throws Exception {
        JSONObject c = json.optJSONObject("customer");
        if (c != null) {
            tvName.setText(c.optString("full_name", "—"));
            tvEmail.setText(c.optString("email", "—"));
            String phone = c.optString("phone", "");
            tvPhone.setText(phone.isEmpty() ? "Not provided" : phone);
            String createdAt = c.optString("created_at", "—");
            tvMemberSince.setText(createdAt.length() >= 10
                    ? createdAt.substring(0, 10) : createdAt);
        }

        JSONArray accounts = json.optJSONArray("accounts");
        llProfileAccounts.removeAllViews();
        if (accounts != null) {
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                View row = getLayoutInflater().inflate(
                        R.layout.item_profile_account, llProfileAccounts, false);
                ((TextView) row.findViewById(R.id.tvAccType))
                        .setText(acc.getString("account_type") + " Account");
                ((TextView) row.findViewById(R.id.tvAccNumber))
                        .setText(acc.getString("account_number"));
                ((TextView) row.findViewById(R.id.tvAccBranch))
                        .setText(acc.getString("branch_name")
                                + "  ·  IFSC: " + acc.getString("ifsc_code"));
                ((TextView) row.findViewById(R.id.tvAccBalance))
                        .setText("₹" + String.format("%,.2f", acc.getDouble("balance")));
                llProfileAccounts.addView(row);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
