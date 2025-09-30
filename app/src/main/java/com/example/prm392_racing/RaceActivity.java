package com.example.prm392_racing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

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
    private TextView countdownText;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    // vị trí
    private float pos1 = 0, pos2 = 0, pos3 = 0;

    // trạng thái hoàn thành
    private boolean finished1 = false, finished2 = false, finished3 = false;

    // kết quả (lưu thời điểm về đích)
    private List<Result> results = new ArrayList<>();

    // runnable references để có thể remove callbacks
    private Runnable countdownRunnable;
    private Runnable raceRunnable;

    // trạng thái đua
    private boolean isRacing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        countdownText = findViewById(R.id.countdownText);

        horse1 = findViewById(R.id.horse1);
        horse2 = findViewById(R.id.horse2);
        horse3 = findViewById(R.id.horse3);

        horse1Gif = findViewById(R.id.horse1Gif);
        horse2Gif = findViewById(R.id.horse2Gif);
        horse3Gif = findViewById(R.id.horse3Gif);

        btnStart = findViewById(R.id.btnStart);
        btnRestart = findViewById(R.id.btnRestart);

        // Load GIF động (Glide) — GIF sẽ chạy; vị trí sẽ điều chỉnh khi reset/update
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse1Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse2Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse3Gif);

        // ban đầu: restart disabled
        btnRestart.setEnabled(false);

        btnStart.setOnClickListener(v -> {
            if (!isRacing) {
                // reset về trạng thái ban đầu
                resetRace();

                // disable start while counting
                btnStart.setEnabled(false);
                btnRestart.setEnabled(true);

                // start countdown (no fade)
                startCountdown(this::startRace);
            }
        });

        btnRestart.setOnClickListener(v -> {
            // Hủy mọi countdown / race đang dở, reset vị trí, ẩn countdown
            cancelCountdown();
            stopRace();
            resetRace();

            // Cho phép bấm start lại
            btnStart.setEnabled(true);
            btnRestart.setEnabled(true);
            countdownText.setVisibility(View.GONE);
        });

        // ensure initial visual positions after layout pass
        horse1.post(() -> {
            updateGifPosition(horse1, horse1Gif, 0f);
            updateGifPosition(horse2, horse2Gif, 0f);
            updateGifPosition(horse3, horse3Gif, 0f);
        });
    }

    private void resetRace() {
        pos1 = pos2 = pos3 = 0f;
        horse1.setProgress(0);
        horse2.setProgress(0);
        horse3.setProgress(0);

        finished1 = finished2 = finished3 = false;
        results.clear();

        // đưa GIF về vị trí đầu (cập nhật vị trí ngay)
        updateGifPosition(horse1, horse1Gif, 0f);
        updateGifPosition(horse2, horse2Gif, 0f);
        updateGifPosition(horse3, horse3Gif, 0f);

        // không tự bật isRacing ở đây — isRacing chỉ bật khi startRace() chạy
        isRacing = false;
    }

    private void startRace() {
        // bật lại trạng thái đua
        isRacing = true;

        // đảm bảo start button disabled, restart enabled
        btnStart.setEnabled(false);
        btnRestart.setEnabled(true);

        // tạo runnable đua và post
        raceRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRacing) return;

                // Di chuyển từng ngựa nếu chưa về đích
                if (!finished1) pos1 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished2) pos2 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished3) pos3 += 0.1f + random.nextFloat() * 0.9f;

                horse1.setProgress(Math.min((int) pos1, 100));
                horse2.setProgress(Math.min((int) pos2, 100));
                horse3.setProgress(Math.min((int) pos3, 100));

                updateGifPosition(horse1, horse1Gif, pos1);
                updateGifPosition(horse2, horse2Gif, pos2);
                updateGifPosition(horse3, horse3Gif, pos3);

                // Kiểm tra ngựa về đích; lưu thời điểm về đích
                checkFinishWithTime("Ngựa 1", pos1, 1);
                checkFinishWithTime("Ngựa 2", pos2, 2);
                checkFinishWithTime("Ngựa 3", pos3, 3);

                // Nếu tất cả ngựa xong thì công bố kết quả
                if (finished1 && finished2 && finished3) {
                    announceResults();
                } else {
                    handler.postDelayed(this, 50);
                }
            }
        };

        handler.postDelayed(raceRunnable, 20);
    }

    private void stopRace() {
        isRacing = false;
        if (raceRunnable != null) {
            handler.removeCallbacks(raceRunnable);
            raceRunnable = null;
        }
    }

    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    private void checkFinishWithTime(String name, float pos, int id) {
        if (pos >= 100f) {
            long finishTime = SystemClock.elapsedRealtime(); // thời điểm hoàn thành
            if (id == 1 && !finished1) {
                finished1 = true;
                results.add(new Result(name, finishTime));
            }
            if (id == 2 && !finished2) {
                finished2 = true;
                results.add(new Result(name, finishTime));
            }
            if (id == 3 && !finished3) {
                finished3 = true;
                results.add(new Result(name, finishTime));
            }
        }
    }

    private void announceResults() {
        // dừng race nếu chưa dừng
        stopRace();

        // sắp xếp theo thời điểm hoàn thành (nhỏ nhất là về đầu)
        results.sort(Comparator.comparingLong(r -> r.finishTime));

        // chuẩn bị danh sách hiển thị
        String[] items = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            items[i] = "Top " + (i + 1) + ": " + results.get(i).name;
        }

        // hiển thị dialog kết quả (popup list)
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🏆 Kết quả cuộc đua")
                .setItems(items, null)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // sau khi đóng dialog cho phép start lại
                    btnStart.setEnabled(true);
                    btnRestart.setEnabled(false);
                })
                .setCancelable(false)
                .show();
    }

    private void updateGifPosition(SeekBar seekBar, ImageView gif, float pos) {
        int max = seekBar.getMax();
        float percent = (max == 0) ? 0f : (pos / max);

        int availableWidth = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        int thumbOffset = (int) (percent * availableWidth);

        // dùng post để đảm bảo view đã measured (nếu gọi sớm)
        gif.post(() -> {
            gif.setX(seekBar.getX() + seekBar.getPaddingLeft() + thumbOffset - gif.getWidth() / 2f);
            gif.setY(seekBar.getY() + seekBar.getHeight() / 2f - gif.getHeight() / 2f);
        });
    }

    private void startCountdown(Runnable onFinish) {
        // hủy nếu đang có countdown cũ
        cancelCountdown();

        countdownText.setVisibility(View.VISIBLE);
        final int[] count = {3};
        countdownText.setText(String.valueOf(count[0]));

        // không animation / fade — simple numeric countdown
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    countdownText.setText(String.valueOf(count[0]));
                    handler.postDelayed(this, 1000);
                } else {
                    countdownText.setText("START!");
                    // giữ chữ START một lúc rồi ẩn và bắt đầu race
                    handler.postDelayed(() -> {
                        countdownText.setVisibility(View.GONE);
                        countdownRunnable = null;
                        onFinish.run();
                    }, 700);
                }
            }
        };

        // bắt đầu countdown sau 1s (để hiện "3" trước)
        handler.postDelayed(countdownRunnable, 1000);
    }

    // class để lưu kết quả
    private static class Result {
        String name;
        long finishTime;

        Result(String name, long finishTime) {
            this.name = name;
            this.finishTime = finishTime;
        }
    }
}
