package com.example.smartshopmanager;

import com.example.smartshopmanager.auth.AuthUtils;
import com.example.smartshopmanager.auth.RoleManager;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies PIN hashing/validation and complete Role-Based Access Control (RBAC) policies.
 */
public class RoleManagerTest {

    @Test
    public void pinHashingAndVerification_worksCorrectly() {
        String hash = AuthUtils.hashPin("1234");
        assertNotEquals("1234", hash);
        assertTrue(AuthUtils.verifyPin("1234", hash));
        assertFalse(AuthUtils.verifyPin("9999", hash));
        assertFalse(AuthUtils.verifyPin(null, hash));
    }

    @Test
    public void pinValidation_enforcesDigitLength() {
        assertTrue(AuthUtils.isValidPin("1234"));
        assertTrue(AuthUtils.isValidPin("12345678"));
        assertFalse(AuthUtils.isValidPin("123"));
        assertFalse(AuthUtils.isValidPin("123456789"));
        assertFalse(AuthUtils.isValidPin("12ab"));
        assertFalse(AuthUtils.isValidPin(""));
    }

    @Test
    public void adminPermissions_haveFullAccess() {
        String role = RoleManager.ROLE_ADMIN;
        assertTrue(RoleManager.canDeleteItems(role));
        assertTrue(RoleManager.canAddEditProducts(role));
        assertTrue(RoleManager.canImportApiProducts(role));
        assertTrue(RoleManager.canManageCategories(role));
        assertTrue(RoleManager.canAddEditCustomers(role));
        assertTrue(RoleManager.canManageExpenses(role));
        assertTrue(RoleManager.canViewFinancialOverview(role));
        assertTrue(RoleManager.canViewFinancialReports(role));
        assertTrue(RoleManager.canViewNetProfit(role));
        assertTrue(RoleManager.canEditOrVoidSales(role));
        assertTrue(RoleManager.canManageUsers(role));
    }

    @Test
    public void managerPermissions_blockDeletesNetProfitAndStaffManagement() {
        String role = RoleManager.ROLE_MANAGER;
        assertFalse(RoleManager.canDeleteItems(role));
        assertTrue(RoleManager.canAddEditProducts(role));
        assertTrue(RoleManager.canImportApiProducts(role));
        assertTrue(RoleManager.canManageCategories(role));
        assertTrue(RoleManager.canAddEditCustomers(role));
        assertTrue(RoleManager.canManageExpenses(role));
        assertTrue(RoleManager.canViewFinancialOverview(role));
        assertTrue(RoleManager.canViewFinancialReports(role));
        assertFalse(RoleManager.canViewNetProfit(role));
        assertFalse(RoleManager.canEditOrVoidSales(role));
        assertFalse(RoleManager.canManageUsers(role));
    }

    @Test
    public void cashierPermissions_restrictedToPosAndCustomers() {
        String role = RoleManager.ROLE_CASHIER;
        assertFalse(RoleManager.canDeleteItems(role));
        assertFalse(RoleManager.canAddEditProducts(role));
        assertFalse(RoleManager.canImportApiProducts(role));
        assertFalse(RoleManager.canManageCategories(role));
        assertTrue(RoleManager.canAddEditCustomers(role));
        assertFalse(RoleManager.canManageExpenses(role));
        assertFalse(RoleManager.canViewFinancialOverview(role));
        assertFalse(RoleManager.canViewFinancialReports(role));
        assertFalse(RoleManager.canViewNetProfit(role));
        assertFalse(RoleManager.canEditOrVoidSales(role));
        assertFalse(RoleManager.canManageUsers(role));
    }
}
