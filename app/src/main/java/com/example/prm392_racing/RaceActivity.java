package com.example.prm392_racing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class RaceActivity extends AppCompatActivity {

    private SeekBar horse1, horse2, horse3;
    private Button btnStart;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private boolean isRacing = false;

    // Dùng float để tính tiến độ chính xác hơn
    private float pos1 = 0, pos2 = 0, pos3 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        horse1 = findViewById(R.id.horse1);
        horse2 = findViewById(R.id.horse2);
        horse3 = findViewById(R.id.horse3);
        btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            if (!isRacing) {
                resetRace();
                startRace();
            }
        });
    }

    private void resetRace() {
        pos1 = pos2 = pos3 = 0;
        horse1.setProgress(0);
        horse2.setProgress(0);
        horse3.setProgress(0);
        isRacing = true;
    }

    private void startRace() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRacing) return;

                // Tăng chậm: random từ 0.1 đến 0.5
                pos1 += 0.1f + random.nextFloat() * 0.9f;
                pos2 += 0.1f + random.nextFloat() * 0.9f;
                pos3 += 0.1f + random.nextFloat() * 0.9f;

                horse1.setProgress(Math.min((int) pos1, 100));
                horse2.setProgress(Math.min((int) pos2, 100));
                horse3.setProgress(Math.min((int) pos3, 100));

                if (pos1 >= 100) {
                    announceWinner("Ngựa 1");
                } else if (pos2 >= 100) {
                    announceWinner("Ngựa 2");
                } else if (pos3 >= 100) {
                    announceWinner("Ngựa 3");
                } else {
                    handler.postDelayed(this, 20); // Cập nhật mỗi 20ms
                }
            }
        }, 20);
    }

    private void announceWinner(String winner) {
        isRacing = false;
        Toast.makeText(this, winner + " thắng cuộc!", Toast.LENGTH_LONG).show();
    }
}
