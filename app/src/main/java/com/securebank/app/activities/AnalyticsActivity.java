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

public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";

    private ProgressBar progressBar;
    private LinearLayout llDebitBreakdown, llCreditBreakdown, llMonthly, llAllTime;
    private TextView tvSelPeriod, tvNoDebit, tvNoCredit, tvNoMonthly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics");
        }

        progressBar       = findViewById(R.id.progressBar);
        tvSelPeriod       = findViewById(R.id.tvSelPeriod);
        llDebitBreakdown  = findViewById(R.id.llDebitBreakdown);
        llCreditBreakdown = findViewById(R.id.llCreditBreakdown);
        llMonthly         = findViewById(R.id.llMonthly);
        llAllTime         = findViewById(R.id.llAllTime);
        tvNoDebit         = findViewById(R.id.tvNoDebit);
        tvNoCredit        = findViewById(R.id.tvNoCredit);
        tvNoMonthly       = findViewById(R.id.tvNoMonthly);

        loadAnalytics();
    }

    private void loadAnalytics() {
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getAnalytics()
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
                                renderAnalytics(json);
                            } else {
                                Toast.makeText(AnalyticsActivity.this,
                                        json.optString("message", "Error"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parse error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AnalyticsActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderAnalytics(JSONObject json) throws Exception {
        int selYear  = json.optInt("sel_year");
        int selMonth = json.optInt("sel_month");
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String monthName = (selMonth >= 1 && selMonth <= 12) ? months[selMonth] : "";
        tvSelPeriod.setText(monthName + " " + selYear);

        // Debit breakdown
        JSONArray debits = json.optJSONArray("chart_data");
        llDebitBreakdown.removeAllViews();
        if (debits != null && debits.length() > 0) {
            tvNoDebit.setVisibility(View.GONE);
            double maxVal = 0;
            for (int i = 0; i < debits.length(); i++)
                maxVal = Math.max(maxVal, debits.getJSONObject(i).optDouble("total"));
            for (int i = 0; i < debits.length(); i++) {
                JSONObject item = debits.getJSONObject(i);
                llDebitBreakdown.addView(makeBarRow(
                        item.optString("category"),
                        item.optDouble("total"),
                        item.optInt("txn_count"),
                        maxVal, false));
            }
        } else {
            tvNoDebit.setVisibility(View.VISIBLE);
        }

        // Credit breakdown
        JSONArray credits = json.optJSONArray("credit_chart_data");
        llCreditBreakdown.removeAllViews();
        if (credits != null && credits.length() > 0) {
            tvNoCredit.setVisibility(View.GONE);
            double maxVal = 0;
            for (int i = 0; i < credits.length(); i++)
                maxVal = Math.max(maxVal, credits.getJSONObject(i).optDouble("total"));
            for (int i = 0; i < credits.length(); i++) {
                JSONObject item = credits.getJSONObject(i);
                llCreditBreakdown.addView(makeBarRow(
                        item.optString("category"),
                        item.optDouble("total"),
                        item.optInt("txn_count"),
                        maxVal, true));
            }
        } else {
            tvNoCredit.setVisibility(View.VISIBLE);
        }

        // Monthly totals
        JSONArray monthly = json.optJSONArray("monthly_totals");
        llMonthly.removeAllViews();
        if (monthly != null && monthly.length() > 0) {
            tvNoMonthly.setVisibility(View.GONE);
            for (int i = 0; i < monthly.length(); i++) {
                JSONObject m = monthly.getJSONObject(i);
                llMonthly.addView(makeMonthRow(
                        m.optString("month_label"),
                        m.optDouble("total_debit"),
                        m.optDouble("total_credit")));
            }
        } else {
            tvNoMonthly.setVisibility(View.VISIBLE);
        }

        // All-time
        JSONArray allTime = json.optJSONArray("all_time_data");
        llAllTime.removeAllViews();
        if (allTime != null && allTime.length() > 0) {
            double maxVal = 0;
            for (int i = 0; i < allTime.length(); i++)
                maxVal = Math.max(maxVal, allTime.getJSONObject(i).optDouble("total"));
            for (int i = 0; i < allTime.length(); i++) {
                JSONObject item = allTime.getJSONObject(i);
                llAllTime.addView(makeBarRow(
                        item.optString("category"),
                        item.optDouble("total"),
                        item.optInt("txn_count"),
                        maxVal, null));
            }
        }
    }

    private View makeBarRow(String category, double total, int count,
                            double maxVal, Boolean isCredit) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 0, 0, dp(14));

        // Label row
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setPadding(0, 0, 0, dp(4));

        TextView tvCat = new TextView(this);
        tvCat.setText(category);
        tvCat.setTextColor(getColor(R.color.text_primary));
        tvCat.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvCat.setLayoutParams(lp);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(String.format("₹%,.2f  (%d)", total, count));
        tvAmt.setTextSize(12);
        int color = isCredit == null ? R.color.accent :
                    isCredit ? R.color.credit_green : R.color.debit_red;
        tvAmt.setTextColor(getColor(color));

        labelRow.addView(tvCat);
        labelRow.addView(tvAmt);

        // Bar
        LinearLayout barBg = new LinearLayout(this);
        barBg.setBackgroundColor(getColor(R.color.divider));
        int barW = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
        int fillW = maxVal > 0 ? (int) (barW * (total / maxVal)) : 0;

        View fill = new View(this);
        fill.setBackgroundColor(getColor(color));
        LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(fillW, dp(6));
        fill.setLayoutParams(fillLp);

        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        barBg.setLayoutParams(barLp);
        barBg.addView(fill);

        row.addView(labelRow);
        row.addView(barBg);
        return row;
    }

    private View makeMonthRow(String label, double debit, double credit) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getColor(R.color.text_secondary));
        tvLabel.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lp);

        TextView tvDebit = new TextView(this);
        tvDebit.setText(String.format("-₹%,.0f", debit));
        tvDebit.setTextColor(getColor(R.color.debit_red));
        tvDebit.setTextSize(12);
        tvDebit.setPadding(0, 0, dp(12), 0);

        TextView tvCredit = new TextView(this);
        tvCredit.setText(String.format("+₹%,.0f", credit));
        tvCredit.setTextColor(getColor(R.color.credit_green));
        tvCredit.setTextSize(12);

        row.addView(tvLabel);
        row.addView(tvDebit);
        row.addView(tvCredit);

        // Bottom divider
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);
        View div = new View(this);
        div.setBackgroundColor(getColor(R.color.divider));
        div.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        wrapper.addView(div);
        return wrapper;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
