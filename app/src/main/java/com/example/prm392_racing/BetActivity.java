package com.example.prm392_racing;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class BetActivity extends AppCompatActivity {
    private TextView tvBalance, tvTile1, tvTile2, tvTile3;
    private Button btnTopUp, btnPlay;
    private CheckBox cbHorse1, cbHorse2, cbHorse3;
    private EditText edtHorse1, edtHorse2, edtHorse3;

    private int balance = 1000; // số dư ban đầu
    private int winningsBalance;
    private ActivityResultLauncher<Intent> raceLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bet);

        tvBalance = findViewById(R.id.tvBalance);
        tvTile1 = findViewById(R.id.tvTile1);
        tvTile2 = findViewById(R.id.tvTile2);
        tvTile3 = findViewById(R.id.tvTile3);
        btnTopUp = findViewById(R.id.btnTopUp);
        btnPlay = findViewById(R.id.btnPlay);

        cbHorse1 = findViewById(R.id.cbHorse1);
        cbHorse2 = findViewById(R.id.cbHorse2);
        cbHorse3 = findViewById(R.id.cbHorse3);

        edtHorse1 = findViewById(R.id.edtHorse1);
        edtHorse2 = findViewById(R.id.edtHorse2);
        edtHorse3 = findViewById(R.id.edtHorse3);

        double odd1 = randomOdds(1.5, 2.5);
        double odd2 = randomOdds(4.0, 6.0);
        double odd3 = randomOdds(2.5, 4.0);

        tvTile1.setText("Tỉ lệ cược: " + String.format("%.1f", odd1));
        tvTile2.setText("Tỉ lệ cược: " + String.format("%.1f", odd2));
        tvTile3.setText("Tỉ lệ cược: " + String.format("%.1f", odd3));

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

        raceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        int newBalance = result.getData().getIntExtra("winningsBalance", balance);
                        balance = newBalance;
                        updateBalance();
                    }
                }
        );

        btnPlay.setOnClickListener(v -> {

            int bet1 = cbHorse1.isChecked() && !edtHorse1.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse1.getText().toString()) : 0;

            int bet2 = cbHorse2.isChecked() && !edtHorse2.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse2.getText().toString()) : 0;

            int bet3 = cbHorse3.isChecked() && !edtHorse3.getText().toString().isEmpty()
                    ? Integer.parseInt(edtHorse3.getText().toString()) : 0;

            int totalBet = bet1 + bet2 + bet3;

            if (totalBet == 0) {
                Toast.makeText(this, "Bạn chưa đặt cược!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (totalBet > balance) {
                Toast.makeText(this, "Không đủ số dư để đặt cược!", Toast.LENGTH_SHORT).show();
                return;
            }

            balance -= totalBet;

            updateBalance();

            Intent intent = new Intent(BetActivity.this, RaceActivity.class);

            intent.putExtra("bet_horse1", bet1);
            intent.putExtra("bet_horse2", bet2);
            intent.putExtra("bet_horse3", bet3);
            intent.putExtra("odd_1", odd1);
            intent.putExtra("odd_2", odd2);
            intent.putExtra("odd_3", odd3);
            intent.putExtra("balance", balance);

            raceLauncher.launch(intent);
        });
    }

    private double randomOdds(double min, double max) {
        return min + (Math.random() * (max - min));
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