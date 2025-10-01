package com.example.prm392_racing;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignInActivity extends AppCompatActivity {

    private EditText editTextUser, editTextPassword;
    private TextView tvStart, horseRacing;

    private final String VALID_USERNAME = "group3";
    private final String VALID_PASSWORD = "123456";

    private MediaPlayer mediaPlayer;
    private ViewFlipper bgFlipper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Views
        editTextUser = findViewById(R.id.editTextUser);
        editTextPassword = findViewById(R.id.editTextPassword);
        tvStart = findViewById(R.id.tvStart);
        horseRacing = findViewById(R.id.horseRacing);
        bgFlipper = findViewById(R.id.bgFlipper);

        // Background slideshow
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        bgFlipper.setInAnimation(in);
        bgFlipper.setOutAnimation(out);
        bgFlipper.startFlipping();

        // Title animations
        startFloatingTitle();
        startShimmerTitle();

        // Button animations
        startPulseAnimation(tvStart);

        mediaPlayer = MediaPlayer.create(this, R.raw.login_backgrounf);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Click handler
        tvStart.setOnClickListener(v -> {
            startClickAnimation(tvStart);
            login();
        });
    }

    private void startFloatingTitle() {
        TranslateAnimation floatAnim = new TranslateAnimation(0, 0, -20, 20);
        floatAnim.setDuration(3000);
        floatAnim.setRepeatCount(Animation.INFINITE);
        floatAnim.setRepeatMode(Animation.REVERSE);

        AlphaAnimation fadeAnim = new AlphaAnimation(0.8f, 1.0f);
        fadeAnim.setDuration(3000);
        fadeAnim.setRepeatCount(Animation.INFINITE);
        fadeAnim.setRepeatMode(Animation.REVERSE);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(floatAnim);
        set.addAnimation(fadeAnim);

        horseRacing.startAnimation(set);
    }

    private void startShimmerTitle() {
        horseRacing.post(() -> {
            float textWidth = horseRacing.getPaint().measureText(horseRacing.getText().toString());
            float textSize = horseRacing.getTextSize();

            ValueAnimator animator = ValueAnimator.ofFloat(0, textWidth * 2);
            animator.setDuration(3000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(animation -> {
                float translate = (float) animation.getAnimatedValue();
                Shader shader = new LinearGradient(
                        translate, 0, translate + textWidth, textSize,
                        new int[]{
                                Color.parseColor("#FFFACD"),
                                Color.parseColor("#FFD700"),
                                Color.parseColor("#FFFACD")
                        },
                        null, Shader.TileMode.CLAMP);

                horseRacing.getPaint().setShader(shader);
                horseRacing.invalidate();
            });
            animator.start();
        });
    }

    private void startPulseAnimation(View view) {
        AlphaAnimation pulse = new AlphaAnimation(0.85f, 1.0f);
        pulse.setDuration(1000);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        view.startAnimation(pulse);
    }

    private void startClickAnimation(View view) {
        ScaleAnimation scale = new ScaleAnimation(
                1.0f, 0.92f, 1.0f, 0.92f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(150);
        scale.setRepeatCount(1);
        scale.setRepeatMode(Animation.REVERSE);
        view.startAnimation(scale);
    }

    private void login() {
        String username = editTextUser.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = MediaPlayer.create(this, R.raw.login);
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
            mediaPlayer.start();

            Intent intent = new Intent(SignInActivity.this, BetActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Sai username hoặc password!", Toast.LENGTH_SHORT).show();
        }
    }
}
