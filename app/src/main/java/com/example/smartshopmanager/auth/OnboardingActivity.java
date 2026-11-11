package com.example.smartshopmanager.auth;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartshopmanager.R;
import com.example.smartshopmanager.database.DatabaseHelper;

/**
 * First-run multi-step onboarding tour introducing features, roles, and seeded demo data.
 */
public class OnboardingActivity extends AppCompatActivity {

    public static final String EXTRA_REPLAY = "extra_replay_onboarding";

    private static class Slide {
        final String emoji;
        final String badge;
        final String title;
        final String body;
        final String highlights;

        Slide(String emoji, String badge, String title, String body, String highlights) {
            this.emoji = emoji;
            this.badge = badge;
            this.title = title;
            this.body = body;
            this.highlights = highlights;
        }
    }

    private final Slide[] slides = new Slide[]{
            new Slide(
                    "🌿",
                    "ALL-IN-ONE SHOP SUITE",
                    "Welcome to Smart Shop Manager",
                    "Run your entire storefront from a clean bottom navigation bar covering Home, Products, POS Cart, Customers, and Finance.",
                    "• Full Products & Categories CRUD + API Import\n• Customer Directory & Purchase History\n• Offline SQLite Database Persistence"
            ),
            new Slide(
                    "🔐",
                    "ROLE-BASED ACCESS CONTROL",
                    "Tailored Permissions Per Role",
                    "Every screen, button, and database action adapts automatically to the signed-in staff role.",
                    "• Admin: Full CRUD, Staff Accounts & Net Profit\n• Manager: Inventory, Expenses & Reports\n• Cashier: POS Checkout & Customers Only"
            ),
            new Slide(
                    "🛍️",
                    "SMART POS & INVENTORY SYNC",
                    "Fast Checkout & Atomic Stock",
                    "Build orders with inline quantity steppers, discounts, and customer selection while stock updates transactionally.",
                    "• Live Inventory Validation at Checkout\n• Itemized Digital Receipts for Every Sale\n• Admin Sale Edits & Void Stock Restoration"
            ),
            new Slide(
                    "📊",
                    "READY-TO-EXPLORE DEMO DATA",
                    "Pre-Loaded Shop & Accounts",
                    "We seeded sample botanical products, customers, sales, expenses, and staff accounts so you can explore right away.",
                    "• Admin: admin / PIN 1234\n• Manager: manager / PIN 2222\n• Cashier: cashier / PIN 3333"
            )
    };

    private int currentIndex = 0;
    private boolean replayMode = false;

    private TextView tvStep, tvEmoji, tvBadge, tvTitle, tvBody, tvHighlights;
    private Button btnSkip, btnBack, btnNext;
    private LinearLayout layoutDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize DB early so default seed data is ready before LoginActivity
        new DatabaseHelper(this).getReadableDatabase();

        replayMode = getIntent().getBooleanExtra(EXTRA_REPLAY, false);
        if (!replayMode && RoleManager.isOnboardingCompleted(this)) {
            proceedToLogin();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        tvStep = findViewById(R.id.tvOnboardingStep);
        tvEmoji = findViewById(R.id.tvOnboardingEmoji);
        tvBadge = findViewById(R.id.tvOnboardingBadge);
        tvTitle = findViewById(R.id.tvOnboardingTitle);
        tvBody = findViewById(R.id.tvOnboardingBody);
        tvHighlights = findViewById(R.id.tvOnboardingHighlights);
        btnSkip = findViewById(R.id.btnOnboardingSkip);
        btnBack = findViewById(R.id.btnOnboardingBack);
        btnNext = findViewById(R.id.btnOnboardingNext);
        layoutDots = findViewById(R.id.layoutOnboardingDots);

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnBack.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                renderSlide();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentIndex < slides.length - 1) {
                currentIndex++;
                renderSlide();
            } else {
                finishOnboarding();
            }
        });

        renderSlide();
    }

    private void renderSlide() {
        Slide slide = slides[currentIndex];
        tvStep.setText("Step " + (currentIndex + 1) + " of " + slides.length);
        tvEmoji.setText(slide.emoji);
        tvBadge.setText(slide.badge);
        tvTitle.setText(slide.title);
        tvBody.setText(slide.body);
        tvHighlights.setText(slide.highlights);

        btnBack.setVisibility(currentIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setText(currentIndex == slides.length - 1 ? "Get Started" : "Next");
        btnSkip.setVisibility(currentIndex == slides.length - 1 ? View.INVISIBLE : View.VISIBLE);

        renderDots();
    }

    private void renderDots() {
        layoutDots.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int activeWidth = (int) (22 * density);
        int margin = (int) (4 * density);
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.mint_badge);

        for (int i = 0; i < slides.length; i++) {
            View dot = new View(this);
            boolean active = (i == currentIndex);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(active ? activeWidth : size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(size / 2f);
            bg.setColor(active ? activeColor : inactiveColor);
            dot.setBackground(bg);

            final int targetIndex = i;
            dot.setOnClickListener(v -> {
                currentIndex = targetIndex;
                renderSlide();
            });
            layoutDots.addView(dot);
        }
    }

    private void finishOnboarding() {
        RoleManager.setOnboardingCompleted(this, true);
        if (replayMode) {
            finish();
        } else {
            proceedToLogin();
        }
    }

    private void proceedToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
