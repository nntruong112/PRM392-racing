package com.example.prm392_racing;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
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
    private TextView countdownText, tvBet1, tvBet2, tvBet3, tvHorse1Odds, tvHorse2Odds, tvHorse3Odds, tvHorse1Name, tvHorse2Name, tvHorse3Name;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    private android.media.MediaPlayer mediaPlayer;

    // vị trí
    private float pos1 = 0, pos2 = 0, pos3 = 0;

    private boolean boostEnabled = false;        // set via Intent or UI
    private float speedMult = 1.0f;

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

        tvHorse1Name = findViewById(R.id.tvHorse1);
        tvHorse2Name = findViewById(R.id.tvHorse2);
        tvHorse3Name = findViewById(R.id.tvHorse3);

        tvHorse1Name.setText("Haru Urara");
        tvHorse2Name.setText("Special Week");
        tvHorse3Name.setText("Symboli Rudolf");

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
        Glide.with(this).asGif().load(R.drawable.horse1).into(horse1Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse2Gif);
        Glide.with(this).asGif().load(R.drawable.horse3).into(horse3Gif);

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

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.release();
                mediaPlayer = null;
            }


            stopRace();
            resetRace();

            // Cho phép bấm start lại
            btnStart.setEnabled(true);
            btnRestart.setEnabled(false);
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

        mediaPlayer = MediaPlayer.create(this, R.raw.racing_music);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        mediaPlayer.start();


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
                checkFinishWithTime("Haru Urara", pos1, 1);
                checkFinishWithTime("Special Week", pos2, 2);
                checkFinishWithTime("Symboli Rudolf", pos3, 3);

                // Nếu tất cả ngựa xong thì công bố kết quả
                if (finished1 && finished2 && finished3) {
                    announceResults();
                } else {
                    handler.postDelayed(this, 60);
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

        // Tạo danh sách hiển thị
        StringBuilder resultText = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            resultText.append("Top ").append(i + 1).append(": ")
                    .append(results.get(i).name).append("\n");
        }

        // Lấy ngựa thắng
        String winner = results.get(0).name;

        // Nhận dữ liệu cược + odds từ intent
        int bet1 = getIntent().getIntExtra("bet_horse1", 0);
        int bet2 = getIntent().getIntExtra("bet_horse2", 0);
        int bet3 = getIntent().getIntExtra("bet_horse3", 0);

        double odd1 = getIntent().getDoubleExtra("odd_1", 1.0);
        double odd2 = getIntent().getDoubleExtra("odd_2", 1.0);
        double odd3 = getIntent().getDoubleExtra("odd_3", 1.0);

        int balance = getIntent().getIntExtra("balance", 0);
        int payout = 0;

        if (winner.equals("Haru Urara") && bet1 > 0) {
            payout = (int) (bet1 * (odd1 - 1));
            balance += payout;
        } else if (winner.equals("Special Week") && bet2 > 0) {
            payout = (int) (bet2 * (odd2 - 1));
            balance += payout;
        } else if (winner.equals("Symboli Rudolf") && bet3 > 0) {
            payout = (int) (bet3 * (odd3 - 1));
            balance += payout;
        }

        resultText.append("\n");

        if (payout > 0) {
            resultText.append("✅ Bạn thắng cược!\n")
                    .append("Nhận về: ").append(payout).append("$\n")
                    .append("Số dư mới: ").append(balance).append("$");
        } else {
            resultText.append("❌ Bạn thua cược!\n")
                    .append("Số dư còn: ").append(balance).append("$");
        }

        // Tạo dialog popup
        int finalBalance = balance;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🏆 Kết quả cuộc đua")
                .setMessage(resultText.toString())
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
// sau khi đóng dialog cho phép start lại
                    btnStart.setEnabled(true);
                    btnRestart.setEnabled(false);

                    Intent resultIntent = new Intent(RaceActivity.this, BetActivity.class);
                    resultIntent.putExtra("winningsBalance", finalBalance);
                    setResult(RESULT_OK, resultIntent);

                    finish();
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
