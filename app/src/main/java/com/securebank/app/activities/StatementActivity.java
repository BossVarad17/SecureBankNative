package com.securebank.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.securebank.app.R;
import com.securebank.app.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatementActivity extends AppCompatActivity {

    private static final String TAG = "StatementActivity";

    private RecyclerView rvTransactions;
    private ProgressBar progressBar;
    private TextView tvAccountTitle, tvBalance, tvIfsc, tvPageInfo, tvEmpty;
    private Button btnPrev, btnNext;

    private int accountId, currentPage = 1, totalPages = 1;
    private TransactionAdapter adapter;
    private final List<JSONObject> transactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statement);

        accountId = getIntent().getIntExtra("account_id", -1);
        String accountNumber = getIntent().getStringExtra("account_number");
        String accountType   = getIntent().getStringExtra("account_type");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Statement");
        }

        tvAccountTitle = findViewById(R.id.tvAccountTitle);
        tvBalance      = findViewById(R.id.tvBalance);
        tvIfsc         = findViewById(R.id.tvIfsc);
        tvPageInfo     = findViewById(R.id.tvPageInfo);
        tvEmpty        = findViewById(R.id.tvEmpty);
        btnPrev        = findViewById(R.id.btnPrev);
        btnNext        = findViewById(R.id.btnNext);
        progressBar    = findViewById(R.id.progressBar);
        rvTransactions = findViewById(R.id.rvTransactions);

        tvAccountTitle.setText(accountType + " Account  ·  " + accountNumber);

        adapter = new TransactionAdapter(transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        btnPrev.setOnClickListener(v -> { if (currentPage > 1)          { currentPage--; loadStatement(); } });
        btnNext.setOnClickListener(v -> { if (currentPage < totalPages)  { currentPage++; loadStatement(); } });

        loadStatement();
    }

    private void loadStatement() {
        progressBar.setVisibility(View.VISIBLE);
        rvTransactions.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        ApiClient.getApiService().getStatement(accountId, currentPage)
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
                                renderStatement(json);
                            } else {
                                Toast.makeText(StatementActivity.this,
                                        json.optString("message", "Error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Statement parse error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StatementActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderStatement(JSONObject json) throws Exception {
        JSONObject account = json.optJSONObject("account");
        if (account != null) {
            tvBalance.setText("Balance: ₹" + String.format("%,.2f", account.getDouble("balance")));
            tvIfsc.setText("IFSC: " + account.getString("ifsc_code"));
        }
        totalPages = json.optInt("total_pages", 1);
        int totalRecords = json.optInt("total_records", 0);
        tvPageInfo.setText("Page " + currentPage + " of " + totalPages
                + "  (" + totalRecords + " transactions)");
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);

        transactions.clear();
        JSONArray txns = json.optJSONArray("transactions");
        if (txns != null) {
            for (int i = 0; i < txns.length(); i++) {
                transactions.add(txns.getJSONObject(i));
            }
        }
        adapter.notifyDataSetChanged();

        if (transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvTransactions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
        private final List<JSONObject> data;
        TransactionAdapter(List<JSONObject> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                JSONObject t = data.get(pos);
                String direction = t.optString("direction", "");
                boolean isDebit  = "DEBIT".equals(direction);
                double amount    = t.optDouble("amount", 0.0);

                h.tvType.setText(t.optString("transaction_type", "—"));
                // Format date: "2026-03-10 18:04:35.625584" → "10 Mar 2026, 06:04 PM"
                String rawDate = t.optString("transaction_date", "");
                String displayDate = "—";
                if (!rawDate.isEmpty() && !rawDate.equals("null")) {
                    try {
                        // Take only "2026-03-10 18:04" part
                        String trimmed = rawDate.length() > 16 ? rawDate.substring(0, 16) : rawDate;
                        java.text.SimpleDateFormat inFmt  =
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                        java.text.SimpleDateFormat outFmt =
                            new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
                        displayDate = outFmt.format(inFmt.parse(trimmed));
                    } catch (Exception ignored) {
                        displayDate = rawDate.length() > 16 ? rawDate.substring(0, 16) : rawDate;
                    }
                }
                h.tvDate.setText(displayDate);
                h.tvCounterparty.setText(t.optString("counterparty", "—"));
                h.tvDirection.setText(direction);
                h.tvAmount.setText((isDebit ? "−" : "+") + "₹"
                        + String.format("%,.2f", amount));
                h.tvAmount.setTextColor(h.itemView.getContext().getColor(
                        isDebit ? R.color.debit_red : R.color.credit_green));
                h.tvStatus.setText(t.optString("status", ""));
            } catch (Exception ignored) {}
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvDate, tvCounterparty, tvDirection, tvAmount, tvStatus;
            VH(View v) {
                super(v);
                tvType         = v.findViewById(R.id.tvType);
                tvDate         = v.findViewById(R.id.tvDate);
                tvCounterparty = v.findViewById(R.id.tvCounterparty);
                tvDirection    = v.findViewById(R.id.tvDirection);
                tvAmount       = v.findViewById(R.id.tvAmount);
                tvStatus       = v.findViewById(R.id.tvStatus);
            }
        }
    }
}
