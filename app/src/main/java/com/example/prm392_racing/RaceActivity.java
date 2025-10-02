package com.example.prm392_racing;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private Button btnStart, btnRestart, btnQuit;
    private TextView countdownText;

    private MediaPlayer bgmPlayer;

    // horse name floating above GIF
    private TextView tvHorse1Name, tvHorse2Name, tvHorse3Name;

    // scoreboard top-left
    private TextView tvHorse1Board, tvHorse2Board, tvHorse3Board;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    private MediaPlayer mediaPlayer;

    private float pos1 = 0, pos2 = 0, pos3 = 0;
    private boolean finished1 = false, finished2 = false, finished3 = false;
    private boolean isRacing = false;

    private List<Result> results = new ArrayList<>();
    private Runnable countdownRunnable;
    private Runnable raceRunnable;

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

        tvHorse1Board = findViewById(R.id.tvHorse1Board);
        tvHorse2Board = findViewById(R.id.tvHorse2Board);
        tvHorse3Board = findViewById(R.id.tvHorse3Board);

        tvHorse1Name.setText("Haru Urara");
        tvHorse2Name.setText("Special Week");
        tvHorse3Name.setText("Symboli Rudolf");

        horse1Gif = findViewById(R.id.horse1Gif);
        horse2Gif = findViewById(R.id.horse2Gif);
        horse3Gif = findViewById(R.id.horse3Gif);

        btnStart = findViewById(R.id.btnStart);
        btnRestart = findViewById(R.id.btnRestart);
        btnQuit = findViewById(R.id.btnQuit);

        Intent intent = getIntent();

        int bet1 = intent.getIntExtra("bet_horse1", 0);
        int bet2 = intent.getIntExtra("bet_horse2", 0);
        int bet3 = intent.getIntExtra("bet_horse3", 0);

        double odd1 = intent.getDoubleExtra("odd_1", 0);
        double odd2 = intent.getDoubleExtra("odd_2", 0);
        double odd3 = intent.getDoubleExtra("odd_3", 0);

        // scoreboard text
        tvHorse1Board.setText("Haru Urara | x" + String.format("%.1f", odd1) + " | " + bet1 + "$");
        tvHorse2Board.setText("Special Week | x" + String.format("%.1f", odd2) + " | " + bet2 + "$");
        tvHorse3Board.setText("Symboli Rudolf | x" + String.format("%.1f", odd3) + " | " + bet3 + "$");

        // load GIF horses
        Glide.with(this).asGif().load(R.drawable.horse1).into(horse1Gif);
        Glide.with(this).asGif().load(R.drawable.horse2).into(horse2Gif);
        Glide.with(this).asGif().load(R.drawable.horse3).into(horse3Gif);

        btnRestart.setEnabled(false);

        btnStart.setOnClickListener(v -> {
            if (!isRacing) {
                resetRace();
                btnStart.setEnabled(false);
                btnRestart.setEnabled(true);
                startCountdown(this::startRace);
            }
        });

        btnRestart.setOnClickListener(v -> {
            cancelCountdown();

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            stopRace();
            resetRace();

            btnStart.setEnabled(true);
            btnRestart.setEnabled(false);
            countdownText.setVisibility(View.GONE);
        });

        btnQuit.setOnClickListener(v -> {
            // Return to BetActivity with current balance (optional)
            Intent resultIntent = new Intent(RaceActivity.this, BetActivity.class);
            resultIntent.putExtra("winningsBalance", getIntent().getIntExtra("balance", 0));
            setResult(RESULT_OK, resultIntent);

            finish(); // Close RaceActivity
        });

        horse1.post(() -> {
            updateGifPosition(horse1, horse1Gif, 0f);
            updateGifPosition(horse2, horse2Gif, 0f);
            updateGifPosition(horse3, horse3Gif, 0f);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startBgm(); // play while RaceActivity is visible (pre-race, countdown, race, result dialog)
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopBgm();  // stop when navigating away (e.g., finishing and returning to BetActivity)
    }

    private void startBgm() {
        if (bgmPlayer == null) {
            // reuse your existing racing music file
            bgmPlayer = MediaPlayer.create(this, R.raw.playing_bg_music);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(0.5f, 0.5f);
            }
        }
        if (bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    private void stopBgm() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) bgmPlayer.stop();
            } catch (IllegalStateException ignored) { }
            bgmPlayer.release();
            bgmPlayer = null;
        }
    }

    private void resetRace() {

        startBgm();

        pos1 = pos2 = pos3 = 0f;
        horse1.setProgress(0);
        horse2.setProgress(0);
        horse3.setProgress(0);

        finished1 = finished2 = finished3 = false;
        results.clear();

        updateGifPosition(horse1, horse1Gif, 0f);
        updateGifPosition(horse2, horse2Gif, 0f);
        updateGifPosition(horse3, horse3Gif, 0f);

        isRacing = false;
    }

    private void startRace() {
        stopBgm();

        isRacing = true;

        mediaPlayer = MediaPlayer.create(this, R.raw.racing_music);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();

        btnStart.setEnabled(false);
        btnRestart.setEnabled(true);

        raceRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRacing) return;

                if (!finished1) pos1 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished2) pos2 += 0.1f + random.nextFloat() * 0.9f;
                if (!finished3) pos3 += 0.1f + random.nextFloat() * 0.9f;

                horse1.setProgress(Math.min((int) pos1, 100));
                horse2.setProgress(Math.min((int) pos2, 100));
                horse3.setProgress(Math.min((int) pos3, 100));

                updateGifPosition(horse1, horse1Gif, pos1);
                updateGifPosition(horse2, horse2Gif, pos2);
                updateGifPosition(horse3, horse3Gif, pos3);

                checkFinishWithTime("Haru Urara", pos1, 1);
                checkFinishWithTime("Special Week", pos2, 2);
                checkFinishWithTime("Symboli Rudolf", pos3, 3);

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
            long finishTime = SystemClock.elapsedRealtime();
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
        stopRace();
        results.sort(Comparator.comparingLong(r -> r.finishTime));

        String winner = results.get(0).name;

        // Bets & odds
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

        int finalBalance = balance;

        // Inflate custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_race_result, null);
        LinearLayout resultContainer = dialogView.findViewById(R.id.resultContainer);
        TextView tvBalanceInfo = dialogView.findViewById(R.id.tvBalanceInfo);

        // Add top results dynamically
        for (int i = 0; i < results.size(); i++) {
            View itemView = getLayoutInflater().inflate(R.layout.item_result_horse, resultContainer, false);

            TextView tvRank = itemView.findViewById(R.id.tvRank);
            ImageView imgHorse = itemView.findViewById(R.id.imgHorse);
            TextView tvHorseName = itemView.findViewById(R.id.tvHorseName);

            tvRank.setText("Top " + (i + 1));
            tvHorseName.setText(results.get(i).name);

            if (results.get(i).name.equals("Haru Urara")) {
                imgHorse.setImageResource(R.drawable.haru_urara);
            } else if (results.get(i).name.equals("Special Week")) {
                imgHorse.setImageResource(R.drawable.special_week);
            } else if (results.get(i).name.equals("Symboli Rudolf")) {
                imgHorse.setImageResource(R.drawable.symboli_rudolf);
            }

            resultContainer.addView(itemView);
        }

        // =========================
        // Balance Info with Highlights
        // =========================
        String text;
        if (payout > 0) {
            text = "✅ Bạn thắng cược!\nNhận về: " + payout + "$\nSố dư mới: " + balance + "$";
        } else {
            text = "❌ Bạn thua cược!\nSố dư còn: " + balance + "$";
        }

        SpannableString ss = new SpannableString(text);

        if (payout > 0) {
            // highlight "thắng cược"
            int start1 = text.indexOf("thắng cược");
            if (start1 >= 0) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                        start1, start1 + "thắng cược".length(), 0);
            }

            // highlight payout number
            String payoutStr = payout + "$";
            int start2 = text.indexOf(payoutStr);
            if (start2 >= 0) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFEB3B")),
                        start2, start2 + payoutStr.length(), 0);
            }
        } else {
            // highlight "thua cược"
            int start1 = text.indexOf("thua cược");
            if (start1 >= 0) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#F44336")),
                        start1, start1 + "thua cược".length(), 0);
            }

            // highlight balance number
            String balanceStr = balance + "$";
            int start2 = text.indexOf(balanceStr);
            if (start2 >= 0) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFEB3B")),
                        start2, start2 + balanceStr.length(), 0);
            }
        }

        // default white for rest
        tvBalanceInfo.setTextColor(Color.WHITE);
        tvBalanceInfo.setText(ss);

        // Show the dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
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

        gif.post(() -> {
            float newX = seekBar.getX() + seekBar.getPaddingLeft() + thumbOffset - gif.getWidth() / 2f;
            float newY = seekBar.getY() + seekBar.getHeight() / 2f - gif.getHeight() / 2f;

            gif.setX(newX);
            gif.setY(newY);

            // move floating name above horse
            TextView nameView;
            if (gif.getId() == R.id.horse1Gif) nameView = tvHorse1Name;
            else if (gif.getId() == R.id.horse2Gif) nameView = tvHorse2Name;
            else nameView = tvHorse3Name;

            if (nameView != null) {
                nameView.setX(newX);
                nameView.setY(newY - nameView.getHeight() - 8);
            }
        });
    }

    private void startCountdown(Runnable onFinish) {
        cancelCountdown();

        countdownText.setVisibility(View.VISIBLE);
        final int[] count = {3};
        countdownText.setText(String.valueOf(count[0]));

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                count[0]--;
                if (count[0] > 0) {
                    countdownText.setText(String.valueOf(count[0]));
                    handler.postDelayed(this, 1000);
                } else {
                    countdownText.setText("START!");
                    handler.postDelayed(() -> {
                        countdownText.setVisibility(View.GONE);
                        countdownRunnable = null;
                        onFinish.run();
                    }, 700);
                }
            }
        };

        handler.postDelayed(countdownRunnable, 1000);
    }

    private static class Result {
        String name;
        long finishTime;

        Result(String name, long finishTime) {
            this.name = name;
            this.finishTime = finishTime;
        }
    }
}
