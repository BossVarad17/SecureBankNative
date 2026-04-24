package com.securebank.app.activities;

import android.content.Intent;
import com.securebank.app.activities.AnalyticsActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.securebank.app.R;
import com.securebank.app.network.ApiClient;
import com.securebank.app.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView tvGreeting, tvLastLogin, tvTotalBalance, tvAccountCount;
    private LinearLayout llAccounts;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        tvGreeting     = findViewById(R.id.tvGreeting);
        tvLastLogin    = findViewById(R.id.tvLastLogin);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvAccountCount = findViewById(R.id.tvAccountCount);
        llAccounts     = findViewById(R.id.llAccounts);
        progressBar    = findViewById(R.id.progressBar);
        swipeRefresh   = findViewById(R.id.swipeRefresh);
        bottomNav      = findViewById(R.id.bottomNav);

        tvGreeting.setText("Hello, " + sessionManager.getUserName().split(" ")[0]);

        swipeRefresh.setOnRefreshListener(this::loadDashboard);
        swipeRefresh.setColorSchemeResources(R.color.primary_purple);

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)     return true;
            if (id == R.id.nav_transfer)  { startActivity(new Intent(this, TransferActivity.class));  return true; }
            if (id == R.id.nav_analytics) { startActivity(new Intent(this, AnalyticsActivity.class)); return true; }
            if (id == R.id.nav_profile)  { startActivity(new Intent(this, ProfileActivity.class));  return true; }
            if (id == R.id.nav_logout)   { confirmLogout(); return true; }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(DashboardActivity.this)
                        .setTitle("Exit App")
                        .setMessage("Do you want to exit SecureBank?")
                        .setPositiveButton("Exit", (d, w) -> finishAffinity())
                        .setNegativeButton("Stay", null)
                        .show();
            }
        });

        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
        loadDashboard();
    }

    private void loadDashboard() {
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getDashboard()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        try {
                            if (response.code() == 401) {
                                sessionManager.clearSession();
                                startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                                finish();
                                return;
                            }
                            ResponseBody rb = response.isSuccessful()
                                    ? response.body() : response.errorBody();
                            if (rb == null) return;
                            String raw = rb.string();
                            Log.d(TAG, "Dashboard: " + raw);
                            JSONObject json = new JSONObject(raw);
                            if ("success".equals(json.optString("status"))) {
                                renderDashboard(json);
                            } else {
                                Toast.makeText(DashboardActivity.this,
                                        json.optString("message", "Error loading dashboard"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Dashboard parse error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Log.e(TAG, "Dashboard failure", t);
                        Toast.makeText(DashboardActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderDashboard(JSONObject json) throws Exception {
        String lastLogin = json.optString("last_login", null);
        tvLastLogin.setText(lastLogin != null && !lastLogin.equals("null")
                ? "Last login: " + lastLogin : "Welcome to SecureBank!");

        double totalBalance = json.optDouble("total_balance", 0.0);
        tvTotalBalance.setText(String.format("₹ %,.2f", totalBalance));

        JSONArray accounts = json.optJSONArray("accounts");
        int count = accounts != null ? accounts.length() : 0;
        tvAccountCount.setText(count + (count == 1 ? " Account" : " Accounts"));

        llAccounts.removeAllViews();
        if (accounts != null) {
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                View card = getLayoutInflater().inflate(
                        R.layout.item_account_card, llAccounts, false);

                ((TextView) card.findViewById(R.id.tvAccountType))
                        .setText(acc.getString("account_type") + " Account");
                ((TextView) card.findViewById(R.id.tvAccountNumber))
                        .setText(acc.getString("account_number"));
                ((TextView) card.findViewById(R.id.tvBranch))
                        .setText(acc.getString("branch_name"));
                ((TextView) card.findViewById(R.id.tvBalance))
                        .setText(String.format("₹ %,.2f", acc.getDouble("balance")));
                ((TextView) card.findViewById(R.id.tvIfsc))
                        .setText("IFSC: " + acc.getString("ifsc_code"));

                int accountId = acc.getInt("account_id");
                String accountNumber = acc.getString("account_number");
                String accountType   = acc.getString("account_type");

                card.findViewById(R.id.tvViewStatement).setOnClickListener(v -> {
                    Intent intent = new Intent(this, StatementActivity.class);
                    intent.putExtra("account_id",     accountId);
                    intent.putExtra("account_number", accountNumber);
                    intent.putExtra("account_type",   accountType);
                    startActivity(intent);
                });
                llAccounts.addView(card);
            }
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> doLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doLogout() {
        ApiClient.getApiService().logout().enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {}
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {}
        });
        ApiClient.clearCookies();
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
