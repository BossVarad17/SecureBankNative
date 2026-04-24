package com.securebank.app.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

public class TransferActivity extends AppCompatActivity {

    private static final String TAG = "TransferActivity";

    private Spinner spinnerFromAccount;
    private EditText etToAccount, etBeneficiaryName, etBankName, etIfsc, etAmount;
    private Button btnTransfer;
    private ProgressBar progressBar;

    private final List<Integer> accountIds = new ArrayList<>();
    private int selectedFromAccountId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Transfer Funds");
        }

        spinnerFromAccount = findViewById(R.id.spinnerFromAccount);
        etToAccount        = findViewById(R.id.etToAccount);
        etBeneficiaryName  = findViewById(R.id.etBeneficiaryName);
        etBankName         = findViewById(R.id.etBankName);
        etIfsc             = findViewById(R.id.etIfsc);
        etAmount           = findViewById(R.id.etAmount);
        btnTransfer        = findViewById(R.id.btnTransfer);
        progressBar        = findViewById(R.id.progressBar);

        btnTransfer.setOnClickListener(v -> attemptTransfer());
        loadTransferData();
    }

    private void loadTransferData() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApiService().getTransferData()
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
                                JSONArray userAccounts = json.optJSONArray("user_accounts");
                                List<String> labels = new ArrayList<>();
                                accountIds.clear();
                                if (userAccounts != null) {
                                    for (int i = 0; i < userAccounts.length(); i++) {
                                        JSONObject acc = userAccounts.getJSONObject(i);
                                        labels.add(acc.getString("account_number")
                                                + " — " + acc.getString("account_type")
                                                + " (₹" + String.format("%,.2f",
                                                acc.getDouble("balance")) + ")");
                                        accountIds.add(acc.getInt("account_id"));
                                    }
                                }
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        TransferActivity.this,
                                        android.R.layout.simple_spinner_item, labels);
                                adapter.setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item);
                                spinnerFromAccount.setAdapter(adapter);
                                spinnerFromAccount.setOnItemSelectedListener(
                                        new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> p, View v,
                                                               int pos, long id) {
                                        selectedFromAccountId = accountIds.get(pos);
                                    }
                                    @Override
                                    public void onNothingSelected(AdapterView<?> p) {}
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Transfer data parse error", e);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TransferActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void attemptTransfer() {
        String toAccount       = etToAccount.getText().toString().trim();
        String beneficiaryName = etBeneficiaryName.getText().toString().trim();
        String bankName        = etBankName.getText().toString().trim();
        String ifsc            = etIfsc.getText().toString().trim();
        String amountStr       = etAmount.getText().toString().trim();

        if (selectedFromAccountId == -1) { Toast.makeText(this, "Select source account", Toast.LENGTH_SHORT).show(); return; }
        if (toAccount.isEmpty())         { etToAccount.setError("Required"); return; }
        if (toAccount.length() != 12)    { etToAccount.setError("Must be 12 digits"); return; }
        if (beneficiaryName.isEmpty())   { etBeneficiaryName.setError("Required"); return; }
        if (bankName.isEmpty())          { etBankName.setError("Required"); return; }
        if (ifsc.isEmpty())              { etIfsc.setError("Required"); return; }
        if (ifsc.length() != 11)         { etIfsc.setError("Must be 11 characters"); return; }
        if (amountStr.isEmpty())         { etAmount.setError("Required"); return; }

        setLoading(true);

        ApiClient.getApiService().doTransfer(
                String.valueOf(selectedFromAccountId),
                toAccount, amountStr, beneficiaryName, bankName, ifsc
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setLoading(false);
                try {
                    ResponseBody rb = response.isSuccessful()
                            ? response.body() : response.errorBody();
                    if (rb == null) return;
                    JSONObject json = new JSONObject(rb.string());
                    String message = json.optString("message", "Transfer failed");
                    Toast.makeText(TransferActivity.this, message, Toast.LENGTH_LONG).show();
                    if ("success".equals(json.optString("status"))) finish();
                } catch (Exception e) {
                    Log.e(TAG, "Transfer parse error", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                Toast.makeText(TransferActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnTransfer.setEnabled(!loading);
        btnTransfer.setText(loading ? "Processing…" : "Submit Transfer");
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
