package com.example.prm392_racing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RaceActivity extends AppCompatActivity {

    private SeekBar horse1, horse2, horse3;
    private ImageView horse1Gif, horse2Gif, horse3Gif;
    private Button btnStart, btnRestart;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private boolean isRacing = false;

    TextView tvBet1, tvBet2, tvBet3, tvHorse1Odds, tvHorse2Odds, tvHorse3Odds;

    // Vị trí
    private float pos1 = 0, pos2 = 0, pos3 = 0;

    // Lưu trạng thái hoàn thành
    private boolean finished1 = false, finished2 = false, finished3 = false;

    // Lưu kết quả
    private List<Result> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        horse1 = findViewById(R.id.horse1);
        horse2 = findViewById(R.id.horse2);
        horse3 = findViewById(R.id.horse3);

        tvBet1 = findViewById(R.id.tvBet1);
        tvBet2 = findViewById(R.id.tvBet2);
        tvBet3 = findViewById(R.id.tvBet3);

        tvHorse1Odds = findViewById(R.id.tvHorse1Odds);
        tvHorse2Odds = findViewById(R.id.tvHorse2Odds);
        tvHorse3Odds = findViewById(R.id.tvHorse3Odds);

        horse1Gif = findViewById(R.id.horse1Gif);
        horse2Gif = findViewById(R.id.horse2Gif);
        horse3Gif = findViewById(R.id.horse3Gif);

        btnStart = findViewById(R.id.btnStart);
        btnRestart = findViewById(R.id.btnRestart);

        Intent intent = getIntent();
        tvBet1.setText(String.valueOf(intent.getIntExtra("bet_horse1", 0)) + "$");
        tvBet2.setText(String.valueOf(intent.getIntExtra("bet_horse2", 0)) + "$");
        tvBet3.setText(String.valueOf(intent.getIntExtra("bet_horse3", 0)) + "$");

        tvHorse1Odds.setText("Tỉ lệ: " + String.format("%.1f", intent.getDoubleExtra("odd_1", 0)) + "x");
        tvHorse2Odds.setText("Tỉ lệ: " + String.format("%.1f", intent.getDoubleExtra("odd_2", 0)) + "x");
        tvHorse3Odds.setText("Tỉ lệ: " + String.format("%.1f", intent.getDoubleExtra("odd_3", 0)) + "x");

        // Load GIF động
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse1Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse2Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse3Gif);

        btnStart.setOnClickListener(v -> {
            if (!isRacing) {
                resetRace();
                startRace();
            }
        });

        btnRestart.setOnClickListener(v -> {
            resetRace();
        });
    }

    private void resetRace() {
        pos1 = pos2 = pos3 = 0;
        horse1.setProgress(0);
        horse2.setProgress(0);
        horse3.setProgress(0);

        finished1 = finished2 = finished3 = false;
        results.clear();

        isRacing = true;
    }

    private void startRace() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRacing) return;

                // Di chuyển từng ngựa
                if (!finished1) pos1 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished2) pos2 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished3) pos3 += 0.1f + random.nextFloat() * 0.9f;

                horse1.setProgress(Math.min((int) pos1, 100));
                horse2.setProgress(Math.min((int) pos2, 100));
                horse3.setProgress(Math.min((int) pos3, 100));

                updateGifPosition(horse1, horse1Gif, pos1);
                updateGifPosition(horse2, horse2Gif, pos2);
                updateGifPosition(horse3, horse3Gif, pos3);

                // Kiểm tra ngựa về đích
                checkFinish("Ngựa 1", pos1, 1);
                checkFinish("Ngựa 2", pos2, 2);
                checkFinish("Ngựa 3", pos3, 3);

                // Nếu tất cả ngựa xong thì công bố kết quả
                if (finished1 && finished2 && finished3) {
                    announceResults();
                } else {
                    handler.postDelayed(this, 50);
                }
            }
        }, 20);
    }

    private void checkFinish(String name, float pos, int id) {
        if (pos >= 100) {
            if (id == 1 && !finished1) {
                finished1 = true;
                results.add(new Result(name, pos));
            }
            if (id == 2 && !finished2) {
                finished2 = true;
                results.add(new Result(name, pos));
            }
            if (id == 3 && !finished3) {
                finished3 = true;
                results.add(new Result(name, pos));
            }
        }
    }

    private void announceResults() {
        isRacing = false;

        // Sắp xếp kết quả theo thứ tự về đích
        results.sort(Comparator.comparingDouble(r -> r.time));

        // Tạo danh sách hiển thị
        String[] items = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            items[i] = "Top " + (i + 1) + ": " + results.get(i).name;
        }

        // Tạo dialog popup
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🏆 Kết quả cuộc đua")
                .setItems(items, null) // danh sách kết quả
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }


    private void updateGifPosition(SeekBar seekBar, ImageView gif, float pos) {
        int max = seekBar.getMax();
        float percent = pos / max;

        int availableWidth = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        int thumbOffset = (int) (percent * availableWidth);

        gif.setX(seekBar.getX() + seekBar.getPaddingLeft() + thumbOffset - gif.getWidth() / 2f);
        gif.setY(seekBar.getY() + seekBar.getHeight() / 2f - gif.getHeight() / 2f);
    }

    // Class nhỏ để lưu kết quả
    private static class Result {
        String name;
        float time;

        Result(String name, float time) {
            this.name = name;
            this.time = time;
        }
    }
}
