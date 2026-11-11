package com.example.smartshopmanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.database.DatabaseHelper;
import com.example.smartshopmanager.models.Category;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Expense;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;
import com.example.smartshopmanager.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation CRUD and transactional integrity tests for DatabaseHelper.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseCrudTest {

    private Context context;
    private DatabaseHelper db;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        db = new DatabaseHelper(context, false);
    }

    @After
    public void tearDown() {
        db.close();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    @Test
    public void seededUsersAndUserCrud_protectLastAdmin() {
        User admin = db.authenticateUser("admin", "1234");
        assertNotNull(admin);
        assertEquals(RoleManager.ROLE_ADMIN, admin.getRole());

        assertNull(db.authenticateUser("admin", "0000"));

        // Last active admin cannot be deleted or demoted
        assertFalse(db.deleteUser(admin.getUserId()));
        admin.setRole(RoleManager.ROLE_CASHIER);
        assertFalse(db.updateUser(admin, null));
        admin.setRole(RoleManager.ROLE_ADMIN);

        // Add, update, and delete a cashier
        long staffId = db.addUser(new User("Alex Cashier", "alex", RoleManager.ROLE_CASHIER, true), "4455");
        assertTrue(staffId > 0);
        User alex = db.authenticateUser("alex", "4455");
        assertNotNull(alex);

        alex.setFullName("Alex Manager");
        alex.setRole(RoleManager.ROLE_MANAGER);
        assertTrue(db.updateUser(alex, "5566"));
        assertNotNull(db.authenticateUser("alex", "5566"));

        assertTrue(db.deleteUser((int) staffId));
        assertNull(db.getUserById((int) staffId));
    }

    @Test
    public void categoryCrud_reassignsProductsToGeneralOnDelete() {
        long catId = db.addCategory("Succulents");
        assertTrue(catId > 0);
        assertEquals(-1, db.addCategory("succulents")); // case-insensitive duplicate guard

        assertTrue(db.updateCategory((int) catId, "Indoor Succulents"));
        long prodId = db.addProduct(new Product("Aloe Vera", (int) catId, 15.0, 8.0, 10));
        assertTrue(prodId > 0);

        assertFalse(db.deleteCategory(1)); // Cannot delete General
        assertTrue(db.deleteCategory((int) catId));

        Product reloaded = db.getProductById((int) prodId);
        assertNotNull(reloaded);
        assertEquals(1, reloaded.getCategoryId());
    }

    @Test
    public void salesLifecycle_verifiesStockUpdatesAndRestoresOnVoid() {
        long custId = db.addCustomer(new Customer("Jane Doe", "555-0101", "jane@shop.com"));
        long prodId = db.addProduct(new Product("Ficus Plant", 1, 30.0, 12.0, 5));

        SaleItem overOrder = new SaleItem((int) prodId, "Ficus Plant", 6, 30.0, 12.0);
        Sale failedSale = new Sale(0, "2026-11-11 10:00:00", 180.0, 0.0, 180.0, "Cash", (int) custId, "Jane Doe", 1, "Shop Owner");
        DatabaseHelper.SaleSaveResult overRes = db.saveSaleDetailed(failedSale, Collections.singletonList(overOrder));
        assertFalse(overRes.success);

        SaleItem validOrder = new SaleItem((int) prodId, "Ficus Plant", 2, 30.0, 12.0);
        Sale validSale = new Sale(0, "2026-11-11 10:05:00", 60.0, 10.0, 54.0, "Card", (int) custId, "Jane Doe", 1, "Shop Owner");
        DatabaseHelper.SaleSaveResult okRes = db.saveSaleDetailed(validSale, Collections.singletonList(validOrder));
        assertTrue(okRes.success);
        assertTrue(okRes.saleId > 0);

        assertEquals(3, db.getProductById((int) prodId).getQuantity());
        assertEquals(24.0, db.getTotalCostOfGoodsSold(), 0.01);
        assertEquals(1, db.getSaleItems((int) okRes.saleId).size());

        // Soft-deleting sold product preserves COGS & receipts while hiding from catalog
        assertTrue(db.deleteProduct((int) prodId));
        assertTrue(db.getAllProducts().isEmpty());
        assertEquals(24.0, db.getTotalCostOfGoodsSold(), 0.01);

        // Update sale metadata & void sale restores stock
        assertTrue(db.updateSale((int) okRes.saleId, 0, "Cash"));
        assertTrue(db.deleteSale((int) okRes.saleId));
        assertEquals(5, db.getProductById((int) prodId).getQuantity());
        assertEquals(0.0, db.getTotalSalesAmount(), 0.01);
    }

    @Test
    public void expenseCrud_andFilteringWork() {
        long expId = db.addExpense(new Expense("Electricity Bill", 120.50, "Utilities", "2026-11-11", "Shop Owner"));
        assertTrue(expId > 0);
        assertEquals(120.50, db.getTotalExpenses(), 0.01);

        List<Expense> list = db.getExpensesFiltered("Electric", "Utilities");
        assertEquals(1, list.size());

        Expense exp = list.get(0);
        exp.setAmount(150.00);
        assertEquals(1, db.updateExpense(exp));
        assertEquals(150.00, db.getTotalExpenses(), 0.01);

        db.deleteExpense(exp.getExpenseId());
        assertEquals(0.0, db.getTotalExpenses(), 0.01);
    }

    @Test
    public void v1ToV2Migration_preservesDataAndBackfillsColumns() {
        db.close();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);

        // Simulate an existing v1 database on device
        SQLiteDatabase legacy = context.openOrCreateDatabase(DatabaseHelper.DATABASE_NAME, Context.MODE_PRIVATE, null);
        legacy.execSQL("CREATE TABLE Categories(category_id INTEGER PRIMARY KEY AUTOINCREMENT, category_name TEXT NOT NULL UNIQUE)");
        legacy.execSQL("CREATE TABLE Products(product_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category_id INTEGER, price REAL NOT NULL, cost_price REAL DEFAULT 0, quantity INTEGER DEFAULT 0)");
        legacy.execSQL("CREATE TABLE Customers(customer_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, email TEXT)");
        legacy.execSQL("CREATE TABLE Sales(sale_id INTEGER PRIMARY KEY AUTOINCREMENT, sale_date TEXT NOT NULL, total_amount REAL NOT NULL, payment_method TEXT, customer_id INTEGER)");
        legacy.execSQL("CREATE TABLE Sale_Items(item_id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity INTEGER NOT NULL, unit_price REAL NOT NULL)");
        legacy.execSQL("CREATE TABLE Expenses(expense_id INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT NOT NULL, amount REAL NOT NULL, category TEXT, date TEXT NOT NULL)");

        legacy.execSQL("INSERT INTO Categories(category_id, category_name) VALUES (1, 'General')");
        legacy.execSQL("INSERT INTO Products(product_id, name, category_id, price, cost_price, quantity) VALUES (1, 'Legacy Fern', 1, 25.0, 10.0, 8)");
        legacy.execSQL("INSERT INTO Sales(sale_id, sale_date, total_amount, payment_method, customer_id) VALUES (1, '2026-11-10 12:00:00', 50.0, 'Cash', NULL)");
        legacy.execSQL("INSERT INTO Sale_Items(item_id, sale_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 2, 25.0)");
        legacy.execSQL("INSERT INTO Expenses(expense_id, description, amount, category, date) VALUES (1, 'Rent', 100.0, 'Rent', '2026-11-10')");
        legacy.setVersion(1);
        legacy.close();

        // Opening v2 DatabaseHelper triggers non-destructive onUpgrade
        db = new DatabaseHelper(context, false);
        assertEquals(1, db.getAllProducts().size());
        assertEquals("Legacy Fern", db.getAllProducts().get(0).getName());
        assertNotNull(db.authenticateUser("admin", "1234"));
        assertEquals(20.0, db.getTotalCostOfGoodsSold(), 0.01);
        List<SaleItem> migratedItems = db.getSaleItems(1);
        assertEquals(1, migratedItems.size());
        assertEquals("Legacy Fern", migratedItems.get(0).getProductName());
        assertEquals(100.0, db.getTotalExpenses(), 0.01);
    }

    @Test
    public void seededDemoShopData_populatesAllModules() {
        db.close();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);

        db = new DatabaseHelper(context); // seedDemoShop = true
        assertEquals(4, db.getAllCategories().size());
        assertEquals(8, db.getAllProducts().size());
        assertEquals(2, db.getLowStockProducts(5).size());
        assertEquals(4, db.getAllCustomers().size());
        assertEquals(3, db.getAllSales().size());
        assertEquals(4, db.getAllExpenses().size());
        assertEquals(140.30, db.getTotalSalesAmount(), 0.01);
        assertEquals(532.50, db.getTotalExpenses(), 0.01);
    }
}
