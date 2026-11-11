package com.example.smartshopmanager.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartshopmanager.MainActivity;
import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.User;

import java.util.List;

/**
 * Staff authentication and account-switching entry point backed by the SQLite Users table.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_SWITCH_ACCOUNT = "extra_switch_account";

    private DatabaseHelper dbHelper;
    private EditText etUsername, etPin;
    private Button btnSubmit, btnCancel;
    private LinearLayout layoutQuickUserChips;
    private TextView tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(this);

        boolean switchMode = getIntent().getBooleanExtra(EXTRA_SWITCH_ACCOUNT, false);
        if (!switchMode && RoleManager.refreshAndValidateSession(this, dbHelper)) {
            navigateToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPin = findViewById(R.id.etLoginPin);
        btnSubmit = findViewById(R.id.btnLoginSubmit);
        btnCancel = findViewById(R.id.btnLoginCancel);
        layoutQuickUserChips = findViewById(R.id.layoutQuickUserChips);
        tvSubtitle = findViewById(R.id.tvLoginSubtitle);

        if (switchMode && RoleManager.isLoggedIn(this)) {
            tvSubtitle.setText("Switch Active Staff Account");
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> finish());
        }

        populateQuickUserChips();
        btnSubmit.setOnClickListener(v -> attemptSignIn());
    }

    private void populateQuickUserChips() {
        layoutQuickUserChips.removeAllViews();
        List<User> activeUsers = dbHelper.getActiveUsers();
        int marginEnd = (int) (8 * getResources().getDisplayMetrics().density);
        int padH = (int) (12 * getResources().getDisplayMetrics().density);
        int padV = (int) (7 * getResources().getDisplayMetrics().density);

        for (User user : activeUsers) {
            TextView chip = new TextView(this);
            chip.setText(user.getFullName() + " • " + user.getRole());
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
            chip.setTextSize(12f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.chip_bg);
            chip.setPadding(padH, padV, padH, padV);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(marginEnd);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                etUsername.setText(user.getUsername());
                etPin.setText("");
                etPin.requestFocus();
            });
            layoutQuickUserChips.addView(chip);
        }
    }

    private void attemptSignIn() {
        String username = etUsername.getText().toString().trim();
        String pin = etPin.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }
        if (pin.isEmpty()) {
            etPin.setError("PIN is required");
            etPin.requestFocus();
            return;
        }

        User authenticated = dbHelper.authenticateUser(username, pin);
        if (authenticated == null) {
            Toast.makeText(this, "Invalid username, PIN, or inactive account", Toast.LENGTH_SHORT).show();
            etPin.setText("");
            etPin.requestFocus();
            return;
        }

        RoleManager.setActiveSession(this, authenticated);
        Toast.makeText(this, "Signed in as " + authenticated.getFullName() + " (" + authenticated.getRole() + ")", Toast.LENGTH_SHORT).show();
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
