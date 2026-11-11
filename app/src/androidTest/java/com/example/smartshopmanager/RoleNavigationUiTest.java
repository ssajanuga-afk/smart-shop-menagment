package com.example.smartshopmanager;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.smartshopmanager.auth.LoginActivity;
import com.example.smartshopmanager.auth.OnboardingActivity;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.auth.UserManagementActivity;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.expense.ExpenseActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end Espresso tests verifying Login, Bottom Navigation replacement, and Role-Based UI.
 */
@RunWith(AndroidJUnit4.class)
public class RoleNavigationUiTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        RoleManager.clearSession(context);
        RoleManager.setOnboardingCompleted(context, true);
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    @After
    public void tearDown() {
        RoleManager.clearSession(context);
    }

    @Test
    public void onboardingFlow_navigatesToLoginAndPersistsCompletion() {
        RoleManager.setOnboardingCompleted(context, false);
        try (ActivityScenario<OnboardingActivity> ignored = ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withId(R.id.tvOnboardingTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.btnOnboardingNext)).perform(click());
            onView(withId(R.id.btnOnboardingNext)).perform(click());
            onView(withId(R.id.btnOnboardingNext)).perform(click());
            onView(withId(R.id.btnOnboardingNext)).perform(click());

            assertTrue(RoleManager.isOnboardingCompleted(context));
            onView(withId(R.id.btnLoginSubmit)).check(matches(isDisplayed()));
        }
    }

    private void signInViaUi(String username, String pin) {
        onView(withId(R.id.etLoginUsername)).perform(scrollTo(), replaceText(username), closeSoftKeyboard());
        onView(withId(R.id.etLoginPin)).perform(scrollTo(), replaceText(pin), closeSoftKeyboard());
        onView(withId(R.id.btnLoginSubmit)).perform(scrollTo(), click());
    }

    @Test
    public void adminFlow_showsFullDashboardBottomNavAndStaffControls() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            signInViaUi("admin", "1234");

            onView(withId(R.id.cardFinancialOverview)).check(matches(isDisplayed()));
            onView(withId(R.id.rowDashboardNetProfit)).check(matches(isDisplayed()));
            onView(withId(R.id.cardAdminControls)).check(matches(isDisplayed()));
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()));
            onView(withId(R.id.bottomNavigation)).check((view, noViewFoundException) -> {
                BottomNavigationView nav = (BottomNavigationView) view;
                assertTrue(nav.getMenu().findItem(R.id.nav_reports).isVisible());
            });

            // Navigate via Bottom Navigation to Products, POS, Customers, Finance
            onView(withId(R.id.nav_products)).perform(click());
            onView(withId(R.id.btnAddProduct)).check(matches(isDisplayed()));
            onView(withId(R.id.btnManageCategories)).check(matches(isDisplayed()));

            onView(withId(R.id.nav_sales)).perform(click());
            onView(withId(R.id.btnCheckout)).check(matches(isDisplayed()));

            onView(withId(R.id.nav_customers)).perform(click());
            onView(withId(R.id.btnAddCustomer)).check(matches(isDisplayed()));

            onView(withId(R.id.nav_reports)).perform(click());
            onView(withId(R.id.cardNetProfit)).check(matches(isDisplayed()));
            onView(withId(R.id.btnReportsManageExpenses)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void managerFlow_hidesNetProfitAndStaffAdmin() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            signInViaUi("manager", "2222");

            onView(withId(R.id.cardFinancialOverview)).check(matches(isDisplayed()));
            onView(withId(R.id.rowDashboardNetProfit)).check(matches(not(isDisplayed())));
            onView(withId(R.id.cardAdminControls)).check(matches(not(isDisplayed())));

            onView(withId(R.id.nav_reports)).perform(click());
            onView(withId(R.id.cardNetProfit)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btnReportsManageExpenses)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cashierFlow_hidesFinancialsAndBlocksRestrictedScreens() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            signInViaUi("cashier", "3333");

            onView(withId(R.id.cardFinancialOverview)).check(matches(not(isDisplayed())));
            onView(withId(R.id.cardCashierShift)).check(matches(isDisplayed()));
            onView(withId(R.id.cardAdminControls)).check(matches(not(isDisplayed())));
            onView(withId(R.id.bottomNavigation)).check((view, noViewFoundException) -> {
                BottomNavigationView nav = (BottomNavigationView) view;
                assertFalse(nav.getMenu().findItem(R.id.nav_reports).isVisible());
            });

            onView(withId(R.id.nav_products)).perform(click());
            onView(withId(R.id.layoutProductActions)).check(matches(not(isDisplayed())));

            onView(withId(R.id.nav_customers)).perform(click());
            onView(withId(R.id.btnAddCustomer)).check(matches(isDisplayed()));
        }

        // Direct launch of restricted activities must reject Cashier
        try (ActivityScenario<ExpenseActivity> expenseScenario = ActivityScenario.launch(
                new Intent(context, ExpenseActivity.class))) {
            assertTrue(expenseScenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED
                    || expenseScenario.getResult() != null);
        }
        try (ActivityScenario<UserManagementActivity> userScenario = ActivityScenario.launch(
                new Intent(context, UserManagementActivity.class))) {
            assertTrue(userScenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED
                    || userScenario.getResult() != null);
        }
    }
}
