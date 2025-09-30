package com.example.prm392_racing;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BetActivity extends AppCompatActivity {
    private TextView tvBalance;
    private Button btnTopUp, btnPlay;
    private CheckBox cbHorse1, cbHorse2, cbHorse3;
    private EditText edtHorse1, edtHorse2, edtHorse3;

    private int balance = 1000; // số dư ban đầu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bet);

        tvBalance = findViewById(R.id.tvBalance);
        btnTopUp = findViewById(R.id.btnTopUp);
        btnPlay = findViewById(R.id.btnPlay);

        cbHorse1 = findViewById(R.id.cbHorse1);
        cbHorse2 = findViewById(R.id.cbHorse2);
        cbHorse3 = findViewById(R.id.cbHorse3);

        edtHorse1 = findViewById(R.id.edtHorse1);
        edtHorse2 = findViewById(R.id.edtHorse2);
        edtHorse3 = findViewById(R.id.edtHorse3);

        updateBalance();

        // Disable EditText ban đầu
        edtHorse1.setEnabled(false);
        edtHorse2.setEnabled(false);
        edtHorse3.setEnabled(false);

        // Xử lý khi tick checkbox
        cbHorse1.setOnCheckedChangeListener((buttonView, isChecked) -> edtHorse1.setEnabled(isChecked));
        cbHorse2.setOnCheckedChangeListener((buttonView, isChecked) -> edtHorse2.setEnabled(isChecked));
        cbHorse3.setOnCheckedChangeListener((buttonView, isChecked) -> edtHorse3.setEnabled(isChecked));

        // Validate tổng tiền không vượt balance
        setupBetWatcher(edtHorse1);
        setupBetWatcher(edtHorse2);
        setupBetWatcher(edtHorse3);

        btnTopUp.setOnClickListener(v -> showTopUpDialog());

        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(BetActivity.this, RaceActivity.class);

            // truyền số tiền cược của từng ngựa (0 nếu không cược)
            intent.putExtra("bet_horse1", cbHorse1.isChecked() && !edtHorse1.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse1.getText().toString()) : 0);

            intent.putExtra("bet_horse2", cbHorse2.isChecked() && !edtHorse2.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse2.getText().toString()) : 0);

            intent.putExtra("bet_horse3", cbHorse3.isChecked() && !edtHorse3.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse3.getText().toString()) : 0);

            // truyền số dư hiện tại
            intent.putExtra("balance", balance);

            startActivity(intent);
        });
    }

    private void updateBalance() {
        tvBalance.setText(balance + "$");
    }

    private void setupBetWatcher(EditText edt) {
        edt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) return;

                int total = getTotalBet();
                if (total > balance) {
                    Toast.makeText(BetActivity.this, "Tổng tiền cược vượt quá số dư!", Toast.LENGTH_SHORT).show();
                    edt.setText(""); // reset ô nhập
                }
            }
        });
    }

    private int getTotalBet() {
        int total = 0;
        if (cbHorse1.isChecked() && !edtHorse1.getText().toString().isEmpty())
            total += Integer.parseInt(edtHorse1.getText().toString());
        if (cbHorse2.isChecked() && !edtHorse2.getText().toString().isEmpty())
            total += Integer.parseInt(edtHorse2.getText().toString());
        if (cbHorse3.isChecked() && !edtHorse3.getText().toString().isEmpty())
            total += Integer.parseInt(edtHorse3.getText().toString());
        return total;
    }

    private void showTopUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_topup, null);
        builder.setView(view);

        EditText edtAmount = view.findViewById(R.id.edtTopUpAmount);
        Button btnConfirm = view.findViewById(R.id.btnConfirmTopUp);

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            String strAmount = edtAmount.getText().toString();
            if (!strAmount.isEmpty()) {
                int add = Integer.parseInt(strAmount);
                balance += add;
                updateBalance();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

}