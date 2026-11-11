package com.example.smartshopmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartshopmanager.auth.AuthUtils;
import com.example.smartshopmanager.auth.RoleManager;
import com.example.smartshopmanager.models.Category;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Expense;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;
import com.example.smartshopmanager.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SmartShop.db";
    public static final int DATABASE_VERSION = 2;
    private final boolean seedDemoShop;

    // Table names
    public static final String TABLE_CATEGORIES = "Categories";
    public static final String TABLE_PRODUCTS = "Products";
    public static final String TABLE_CUSTOMERS = "Customers";
    public static final String TABLE_SALES = "Sales";
    public static final String TABLE_SALE_ITEMS = "Sale_Items";
    public static final String TABLE_EXPENSES = "Expenses";
    public static final String TABLE_USERS = "Users";

    public static class SaleSaveResult {
        public final boolean success;
        public final long saleId;
        public final String errorMessage;

        public SaleSaveResult(boolean success, long saleId, String errorMessage) {
            this.success = success;
            this.saleId = saleId;
            this.errorMessage = errorMessage;
        }
    }

    public DatabaseHelper(Context context) {
        this(context, true);
    }

    public DatabaseHelper(Context context, boolean seedDemoShop) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.seedDemoShop = seedDemoShop;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategories = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + "category_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_name TEXT NOT NULL UNIQUE)";

        String createProducts = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + "product_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "category_id INTEGER, "
                + "price REAL NOT NULL, "
                + "cost_price REAL DEFAULT 0, "
                + "quantity INTEGER DEFAULT 0, "
                + "is_active INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY (category_id) REFERENCES " + TABLE_CATEGORIES + "(category_id))";

        String createCustomers = "CREATE TABLE " + TABLE_CUSTOMERS + "("
                + "customer_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "email TEXT)";

        String createUsers = "CREATE TABLE " + TABLE_USERS + "("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "full_name TEXT NOT NULL, "
                + "username TEXT NOT NULL UNIQUE, "
                + "pin_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "is_active INTEGER NOT NULL DEFAULT 1, "
                + "created_at TEXT NOT NULL)";

        String createSales = "CREATE TABLE " + TABLE_SALES + "("
                + "sale_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_date TEXT NOT NULL, "
                + "subtotal REAL DEFAULT 0, "
                + "discount_percent REAL DEFAULT 0, "
                + "total_amount REAL NOT NULL, "
                + "payment_method TEXT, "
                + "customer_id INTEGER, "
                + "cashier_user_id INTEGER, "
                + "cashier_name TEXT, "
                + "FOREIGN KEY (customer_id) REFERENCES " + TABLE_CUSTOMERS + "(customer_id) ON DELETE SET NULL)";

        String createSaleItems = "CREATE TABLE " + TABLE_SALE_ITEMS + "("
                + "item_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_id INTEGER NOT NULL, "
                + "product_id INTEGER, "
                + "product_name TEXT, "
                + "quantity INTEGER NOT NULL, "
                + "unit_price REAL NOT NULL, "
                + "cost_price REAL DEFAULT 0, "
                + "FOREIGN KEY (sale_id) REFERENCES " + TABLE_SALES + "(sale_id) ON DELETE CASCADE, "
                + "FOREIGN KEY (product_id) REFERENCES " + TABLE_PRODUCTS + "(product_id) ON DELETE SET NULL)";

        String createExpenses = "CREATE TABLE " + TABLE_EXPENSES + "("
                + "expense_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "description TEXT NOT NULL, "
                + "amount REAL NOT NULL, "
                + "category TEXT, "
                + "date TEXT NOT NULL, "
                + "recorded_by TEXT)";

        db.execSQL(createCategories);
        db.execSQL(createProducts);
        db.execSQL(createCustomers);
        db.execSQL(createUsers);
        db.execSQL(createSales);
        db.execSQL(createSaleItems);
        db.execSQL(createExpenses);

        seedDefaultData(db, seedDemoShop);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + "("
                    + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "full_name TEXT NOT NULL, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "pin_hash TEXT NOT NULL, "
                    + "role TEXT NOT NULL, "
                    + "is_active INTEGER NOT NULL DEFAULT 1, "
                    + "created_at TEXT NOT NULL)");

            addColumnIfMissing(db, TABLE_PRODUCTS, "is_active", "INTEGER NOT NULL DEFAULT 1");

            addColumnIfMissing(db, TABLE_SALES, "subtotal", "REAL DEFAULT 0");
            addColumnIfMissing(db, TABLE_SALES, "discount_percent", "REAL DEFAULT 0");
            addColumnIfMissing(db, TABLE_SALES, "cashier_user_id", "INTEGER");
            addColumnIfMissing(db, TABLE_SALES, "cashier_name", "TEXT");
            db.execSQL("UPDATE " + TABLE_SALES + " SET subtotal = total_amount WHERE subtotal = 0 AND total_amount > 0");

            addColumnIfMissing(db, TABLE_SALE_ITEMS, "product_name", "TEXT");
            addColumnIfMissing(db, TABLE_SALE_ITEMS, "cost_price", "REAL DEFAULT 0");
            db.execSQL("UPDATE " + TABLE_SALE_ITEMS + " SET "
                    + "product_name = COALESCE((SELECT p.name FROM " + TABLE_PRODUCTS + " p WHERE p.product_id = " + TABLE_SALE_ITEMS + ".product_id), 'Product'), "
                    + "cost_price = COALESCE((SELECT p.cost_price FROM " + TABLE_PRODUCTS + " p WHERE p.product_id = " + TABLE_SALE_ITEMS + ".product_id), 0) "
                    + "WHERE product_name IS NULL");

            addColumnIfMissing(db, TABLE_EXPENSES, "recorded_by", "TEXT");

            seedDefaultData(db, false);
        }
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
        if (!hasColumn(db, table, column)) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx >= 0 && column.equalsIgnoreCase(c.getString(nameIdx))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void seedDefaultData(SQLiteDatabase db, boolean includeDemoShop) {
        try (Cursor catCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null)) {
            if (catCursor.moveToFirst() && catCursor.getInt(0) == 0) {
                insertSeedCategory(db, "General");
            }
        }

        try (Cursor userCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null)) {
            if (userCursor.moveToFirst() && userCursor.getInt(0) == 0) {
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                insertSeedUser(db, "Shop Owner", "admin", "1234", RoleManager.ROLE_ADMIN, now);
                insertSeedUser(db, "Store Manager", "manager", "2222", RoleManager.ROLE_MANAGER, now);
                insertSeedUser(db, "Front Cashier", "cashier", "3333", RoleManager.ROLE_CASHIER, now);
            }
        }

        if (includeDemoShop) {
            seedDemoShopData(db);
        }
    }

    private void seedDemoShopData(SQLiteDatabase db) {
        if (getTableRowCount(db, TABLE_PRODUCTS) > 0
                || getTableRowCount(db, TABLE_CUSTOMERS) > 0
                || getTableRowCount(db, TABLE_SALES) > 0
                || getTableRowCount(db, TABLE_EXPENSES) > 0) {
            return;
        }

        long catIndoor = insertSeedCategory(db, "Indoor Plants");
        long catSucculents = insertSeedCategory(db, "Succulents");
        long catPots = insertSeedCategory(db, "Pots & Soil");

        long pMonstera = insertSeedProduct(db, "Monstera Deliciosa", catIndoor, 34.00, 16.00, 14);
        long pSnake = insertSeedProduct(db, "Snake Plant", catIndoor, 22.00, 10.00, 18);
        insertSeedProduct(db, "Peace Lily", catIndoor, 26.50, 12.00, 4);
        long pAloe = insertSeedProduct(db, "Aloe Vera Pot", catSucculents, 14.00, 6.00, 20);
        insertSeedProduct(db, "Echeveria Rosette", catSucculents, 11.50, 4.50, 3);
        long pPlanter = insertSeedProduct(db, "Ceramic Planter 8\"", catPots, 18.00, 8.00, 12);
        long pSoil = insertSeedProduct(db, "Organic Potting Mix 5L", catPots, 12.00, 5.00, 25);
        long pMister = insertSeedProduct(db, "Plant Mister Spray", 1, 9.50, 4.00, 15);

        long cMaya = insertSeedCustomer(db, "Maya Lin", "555-0142", "maya.lin@example.com");
        long cLiam = insertSeedCustomer(db, "Liam Carter", "555-0188", "liam.carter@example.com");
        insertSeedCustomer(db, "Sofia Patel", "555-0219", "sofia.p@example.com");
        insertSeedCustomer(db, "Noah Brooks", "555-0274", "noah.b@example.com");

        String today = formatRelativeDate(0, "yyyy-MM-dd");
        String yesterday = formatRelativeDate(-1, "yyyy-MM-dd");
        String twoDaysAgo = formatRelativeDate(-2, "yyyy-MM-dd");
        String threeDaysAgo = formatRelativeDate(-3, "yyyy-MM-dd");

        // Sale 1 (Today - Cashier, Maya Lin, 10% discount)
        long s1 = insertSeedSale(db, today + " 10:15:00", 52.00, 10.0, 46.80, "Card", cMaya, 3, "Front Cashier");
        insertSeedSaleItem(db, s1, pMonstera, "Monstera Deliciosa", 1, 34.00, 16.00);
        insertSeedSaleItem(db, s1, pPlanter, "Ceramic Planter 8\"", 1, 18.00, 8.00);

        // Sale 2 (Today - Cashier, Liam Carter)
        long s2 = insertSeedSale(db, today + " 12:40:00", 56.00, 0.0, 56.00, "Cash", cLiam, 3, "Front Cashier");
        insertSeedSaleItem(db, s2, pSnake, "Snake Plant", 2, 22.00, 10.00);
        insertSeedSaleItem(db, s2, pSoil, "Organic Potting Mix 5L", 1, 12.00, 5.00);

        // Sale 3 (Yesterday - Admin, Walk-in)
        long s3 = insertSeedSale(db, yesterday + " 16:20:00", 37.50, 0.0, 37.50, "Bank Transfer", 0, 1, "Shop Owner");
        insertSeedSaleItem(db, s3, pAloe, "Aloe Vera Pot", 2, 14.00, 6.00);
        insertSeedSaleItem(db, s3, pMister, "Plant Mister Spray", 1, 9.50, 4.00);

        insertSeedExpense(db, "Shop Electricity & Water", 145.00, "Utilities", today, "Store Manager");
        insertSeedExpense(db, "Wholesale Botanical Restock", 280.00, "Inventory", yesterday, "Shop Owner");
        insertSeedExpense(db, "Eco Packaging & Kraft Bags", 62.50, "Supplies", twoDaysAgo, "Store Manager");
        insertSeedExpense(db, "Local Instagram Promotion", 45.00, "Marketing", threeDaysAgo, "Shop Owner");
    }

    private int getTableRowCount(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    private String formatRelativeDate(int dayOffset, String pattern) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayOffset);
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(cal.getTime());
    }

    private long insertSeedCategory(SQLiteDatabase db, String name) {
        ContentValues cv = new ContentValues();
        cv.put("category_name", name);
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private long insertSeedProduct(SQLiteDatabase db, String name, long categoryId, double price, double costPrice, int qty) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("category_id", categoryId > 0 ? categoryId : 1);
        cv.put("price", price);
        cv.put("cost_price", costPrice);
        cv.put("quantity", qty);
        cv.put("is_active", 1);
        return db.insert(TABLE_PRODUCTS, null, cv);
    }

    private long insertSeedCustomer(SQLiteDatabase db, String name, String phone, String email) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("email", email);
        return db.insert(TABLE_CUSTOMERS, null, cv);
    }

    private long insertSeedSale(SQLiteDatabase db, String saleDate, double subtotal, double discount, double total,
                                String method, long customerId, int cashierId, String cashierName) {
        ContentValues cv = new ContentValues();
        cv.put("sale_date", saleDate);
        cv.put("subtotal", subtotal);
        cv.put("discount_percent", discount);
        cv.put("total_amount", total);
        cv.put("payment_method", method);
        if (customerId > 0) cv.put("customer_id", customerId);
        else cv.putNull("customer_id");
        cv.put("cashier_user_id", cashierId);
        cv.put("cashier_name", cashierName);
        return db.insert(TABLE_SALES, null, cv);
    }

    private void insertSeedSaleItem(SQLiteDatabase db, long saleId, long productId, String productName,
                                    int qty, double unitPrice, double costPrice) {
        ContentValues cv = new ContentValues();
        cv.put("sale_id", saleId);
        cv.put("product_id", productId);
        cv.put("product_name", productName);
        cv.put("quantity", qty);
        cv.put("unit_price", unitPrice);
        cv.put("cost_price", costPrice);
        db.insert(TABLE_SALE_ITEMS, null, cv);
    }

    private void insertSeedExpense(SQLiteDatabase db, String desc, double amount, String category, String date, String recordedBy) {
        ContentValues cv = new ContentValues();
        cv.put("description", desc);
        cv.put("amount", amount);
        cv.put("category", category);
        cv.put("date", date);
        cv.put("recorded_by", recordedBy);
        db.insert(TABLE_EXPENSES, null, cv);
    }

    private void insertSeedUser(SQLiteDatabase db, String fullName, String username, String rawPin, String role, String createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("full_name", fullName);
        cv.put("username", username);
        cv.put("pin_hash", AuthUtils.hashPin(rawPin));
        cv.put("role", role);
        cv.put("is_active", 1);
        cv.put("created_at", createdAt);
        db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // =========================================================================
    // USER / STAFF CRUD OPERATIONS
    // =========================================================================

    public long addUser(User user, String rawPin) {
        if (user == null || user.getUsername() == null || user.getFullName() == null) return -1;
        String cleanUsername = user.getUsername().trim().toLowerCase(Locale.ROOT);
        if (cleanUsername.isEmpty() || !AuthUtils.isValidPin(rawPin)) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor check = db.rawQuery("SELECT user_id FROM " + TABLE_USERS + " WHERE LOWER(username) = ?",
                new String[]{cleanUsername})) {
            if (check.moveToFirst()) {
                return -1;
            }
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        ContentValues cv = new ContentValues();
        cv.put("full_name", user.getFullName().trim());
        cv.put("username", cleanUsername);
        cv.put("pin_hash", AuthUtils.hashPin(rawPin));
        cv.put("role", user.getRole());
        cv.put("is_active", user.isActive() ? 1 : 0);
        cv.put("created_at", now);
        return db.insert(TABLE_USERS, null, cv);
    }

    public User authenticateUser(String username, String rawPin) {
        if (username == null || rawPin == null) return null;
        String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE LOWER(username) = ? AND is_active = 1",
                new String[]{cleanUsername})) {
            if (c.moveToFirst()) {
                User user = mapUser(c);
                if (AuthUtils.verifyPin(rawPin, user.getPinHash())) {
                    return user;
                }
            }
        }
        return null;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                return mapUser(c);
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        return queryUsers("SELECT * FROM " + TABLE_USERS + " ORDER BY CASE role WHEN 'Admin' THEN 1 WHEN 'Manager' THEN 2 ELSE 3 END, full_name ASC", null);
    }

    public List<User> getActiveUsers() {
        return queryUsers("SELECT * FROM " + TABLE_USERS + " WHERE is_active = 1 ORDER BY CASE role WHEN 'Admin' THEN 1 WHEN 'Manager' THEN 2 ELSE 3 END, full_name ASC", null);
    }

    private List<User> queryUsers(String sql, String[] args) {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                list.add(mapUser(c));
            }
        }
        return list;
    }

    private User mapUser(Cursor c) {
        int id = c.getInt(c.getColumnIndexOrThrow("user_id"));
        String fullName = c.getString(c.getColumnIndexOrThrow("full_name"));
        String username = c.getString(c.getColumnIndexOrThrow("username"));
        String pinHash = c.getString(c.getColumnIndexOrThrow("pin_hash"));
        String role = c.getString(c.getColumnIndexOrThrow("role"));
        boolean active = c.getInt(c.getColumnIndexOrThrow("is_active")) == 1;
        String createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        return new User(id, fullName, username, pinHash, role, active, createdAt);
    }

    public int getActiveAdminCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_USERS + " WHERE LOWER(role) = 'admin' AND is_active = 1", null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }

    public boolean updateUser(User user, String optionalNewPin) {
        if (user == null || user.getUserId() <= 0) return false;
        User existing = getUserById(user.getUserId());
        if (existing == null) return false;

        boolean wasActiveAdmin = RoleManager.isAdmin(existing.getRole()) && existing.isActive();
        boolean staysActiveAdmin = RoleManager.isAdmin(user.getRole()) && user.isActive();
        if (wasActiveAdmin && !staysActiveAdmin && getActiveAdminCount() <= 1) {
            return false; // Prevent removing or disabling the last active Admin
        }

        String cleanUsername = user.getUsername().trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor dup = db.rawQuery(
                "SELECT user_id FROM " + TABLE_USERS + " WHERE LOWER(username) = ? AND user_id != ?",
                new String[]{cleanUsername, String.valueOf(user.getUserId())})) {
            if (dup.moveToFirst()) {
                return false;
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("full_name", user.getFullName().trim());
        cv.put("username", cleanUsername);
        cv.put("role", user.getRole());
        cv.put("is_active", user.isActive() ? 1 : 0);
        if (optionalNewPin != null && !optionalNewPin.trim().isEmpty()) {
            if (!AuthUtils.isValidPin(optionalNewPin)) return false;
            cv.put("pin_hash", AuthUtils.hashPin(optionalNewPin));
        }
        return db.update(TABLE_USERS, cv, "user_id = ?", new String[]{String.valueOf(user.getUserId())}) > 0;
    }

    public boolean deleteUser(int userId) {
        User existing = getUserById(userId);
        if (existing == null) return false;
        if (RoleManager.isAdmin(existing.getRole()) && existing.isActive() && getActiveAdminCount() <= 1) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USERS, "user_id = ?", new String[]{String.valueOf(userId)}) > 0;
    }

    // =========================================================================
    // CATEGORY CRUD OPERATIONS
    // =========================================================================

    public long addCategory(String name) {
        if (name == null) return -1;
        String clean = name.trim();
        if (clean.isEmpty()) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor check = db.rawQuery(
                "SELECT category_id FROM " + TABLE_CATEGORIES + " WHERE LOWER(category_name) = ?",
                new String[]{clean.toLowerCase(Locale.ROOT)})) {
            if (check.moveToFirst()) {
                return -1;
            }
        }

        ContentValues values = new ContentValues();
        values.put("category_name", clean);
        return db.insert(TABLE_CATEGORIES, null, values);
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY category_id ASC", null)) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
                list.add(new Category(id, name));
            }
        }
        return list;
    }

    public int getProductCountForCategory(int categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE category_id = ? AND is_active = 1",
                new String[]{String.valueOf(categoryId)})) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }

    public boolean updateCategory(int categoryId, String newName) {
        if (newName == null) return false;
        String clean = newName.trim();
        if (clean.isEmpty()) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor dup = db.rawQuery(
                "SELECT category_id FROM " + TABLE_CATEGORIES + " WHERE LOWER(category_name) = ? AND category_id != ?",
                new String[]{clean.toLowerCase(Locale.ROOT), String.valueOf(categoryId)})) {
            if (dup.moveToFirst()) {
                return false;
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("category_name", clean);
        return db.update(TABLE_CATEGORIES, cv, "category_id = ?", new String[]{String.valueOf(categoryId)}) > 0;
    }

    public boolean deleteCategory(int categoryId) {
        if (categoryId <= 1) {
            return false; // Protect default "General" category
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues reassign = new ContentValues();
            reassign.put("category_id", 1);
            db.update(TABLE_PRODUCTS, reassign, "category_id = ?", new String[]{String.valueOf(categoryId)});
            int deleted = db.delete(TABLE_CATEGORIES, "category_id = ?", new String[]{String.valueOf(categoryId)});
            if (deleted > 0) {
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // PRODUCT CRUD OPERATIONS
    // =========================================================================

    public long addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", product.getName().trim());
        values.put("category_id", product.getCategoryId() > 0 ? product.getCategoryId() : 1);
        values.put("price", product.getPrice());
        values.put("cost_price", product.getCostPrice());
        values.put("quantity", product.getQuantity());
        values.put("is_active", 1);
        return db.insert(TABLE_PRODUCTS, null, values);
    }

    public int updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", product.getName().trim());
        values.put("category_id", product.getCategoryId() > 0 ? product.getCategoryId() : 1);
        values.put("price", product.getPrice());
        values.put("cost_price", product.getCostPrice());
        values.put("quantity", product.getQuantity());
        return db.update(TABLE_PRODUCTS, values, "product_id = ?", new String[]{String.valueOf(product.getProductId())});
    }

    /**
     * Deletes an unused product permanently, or archives it (is_active = 0) if referenced in past sales
     * so historical receipts and COGS remain intact.
     */
    public boolean deleteProduct(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean referencedInSales = false;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SALE_ITEMS + " WHERE product_id = ?",
                new String[]{String.valueOf(productId)})) {
            if (c.moveToFirst()) {
                referencedInSales = c.getInt(0) > 0;
            }
        }
        if (referencedInSales) {
            ContentValues cv = new ContentValues();
            cv.put("is_active", 0);
            return db.update(TABLE_PRODUCTS, cv, "product_id = ?", new String[]{String.valueOf(productId)}) > 0;
        }
        return db.delete(TABLE_PRODUCTS, "product_id = ?", new String[]{String.valueOf(productId)}) > 0;
    }

    public Product getProductById(int productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, c.category_name FROM " + TABLE_PRODUCTS + " p LEFT JOIN "
                + TABLE_CATEGORIES + " c ON p.category_id = c.category_id WHERE p.product_id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(productId)})) {
            if (cursor.moveToFirst()) {
                return mapProduct(cursor);
            }
        }
        return null;
    }

    public boolean productExistsByName(String name) {
        if (name == null) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT product_id FROM " + TABLE_PRODUCTS + " WHERE LOWER(name) = ? AND is_active = 1",
                new String[]{name.trim().toLowerCase(Locale.ROOT)})) {
            return c.moveToFirst();
        }
    }

    public List<Product> getAllProducts() {
        return getProductsFiltered(0, "");
    }

    public List<Product> searchProducts(String keyword) {
        return getProductsFiltered(0, keyword);
    }

    public List<Product> getProductsFiltered(int categoryId, String keyword) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT p.*, c.category_name FROM " + TABLE_PRODUCTS
                + " p LEFT JOIN " + TABLE_CATEGORIES + " c ON p.category_id = c.category_id WHERE p.is_active = 1");
        List<String> args = new ArrayList<>();

        if (categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            args.add(String.valueOf(categoryId));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (p.name LIKE ? OR c.category_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY p.name ASC");

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                list.add(mapProduct(cursor));
            }
        }
        return list;
    }

    public List<Product> getLowStockProducts(int threshold) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, c.category_name FROM " + TABLE_PRODUCTS + " p LEFT JOIN "
                + TABLE_CATEGORIES + " c ON p.category_id = c.category_id "
                + "WHERE p.is_active = 1 AND p.quantity <= ? ORDER BY p.quantity ASC, p.name ASC";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(threshold)})) {
            while (cursor.moveToNext()) {
                list.add(mapProduct(cursor));
            }
        }
        return list;
    }

    private Product mapProduct(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("product_id"));
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        int catId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
        double cost = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
        int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
        String catName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
        boolean active = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1;

        Product p = new Product(id, name, catId, price, cost, qty);
        p.setCategoryName(catName != null ? catName : "General");
        p.setActive(active);
        return p;
    }

    // =========================================================================
    // CUSTOMER CRUD OPERATIONS
    // =========================================================================

    public long addCustomer(Customer customer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", customer.getName().trim());
        values.put("phone", customer.getPhone() != null ? customer.getPhone().trim() : "");
        values.put("email", customer.getEmail() != null ? customer.getEmail().trim() : "");
        return db.insert(TABLE_CUSTOMERS, null, values);
    }

    public int updateCustomer(Customer customer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", customer.getName().trim());
        values.put("phone", customer.getPhone() != null ? customer.getPhone().trim() : "");
        values.put("email", customer.getEmail() != null ? customer.getEmail().trim() : "");
        return db.update(TABLE_CUSTOMERS, values, "customer_id = ?", new String[]{String.valueOf(customer.getCustomerId())});
    }

    public void deleteCustomer(int customerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.putNull("customer_id");
            db.update(TABLE_SALES, cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
            db.delete(TABLE_CUSTOMERS, "customer_id = ?", new String[]{String.valueOf(customerId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Customer> getAllCustomers() {
        return searchCustomers("");
    }

    public List<Customer> searchCustomers(String keyword) {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql;
        String[] args;
        if (keyword == null || keyword.trim().isEmpty()) {
            sql = "SELECT * FROM " + TABLE_CUSTOMERS + " ORDER BY name ASC";
            args = null;
        } else {
            String like = "%" + keyword.trim() + "%";
            sql = "SELECT * FROM " + TABLE_CUSTOMERS + " WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY name ASC";
            args = new String[]{like, like, like};
        }

        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("customer_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                list.add(new Customer(id, name, phone, email));
            }
        }
        return list;
    }

    public List<Sale> getSalesByCustomer(int customerId) {
        List<Sale> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT s.*, c.name as customer_name FROM " + TABLE_SALES
                + " s LEFT JOIN " + TABLE_CUSTOMERS + " c ON s.customer_id = c.customer_id "
                + "WHERE s.customer_id = ? ORDER BY s.sale_date DESC, s.sale_id DESC";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(customerId)})) {
            while (cursor.moveToNext()) {
                list.add(mapSale(cursor));
            }
        }
        return list;
    }

    public double getCustomerTotalSpent(int customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COALESCE(SUM(total_amount), 0) FROM " + TABLE_SALES + " WHERE customer_id = ?",
                new String[]{String.valueOf(customerId)})) {
            if (c.moveToFirst()) {
                return c.getDouble(0);
            }
        }
        return 0;
    }

    // =========================================================================
    // EXPENSE CRUD OPERATIONS
    // =========================================================================

    public long addExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", expense.getDescription().trim());
        values.put("amount", expense.getAmount());
        values.put("category", expense.getCategory() != null && !expense.getCategory().trim().isEmpty()
                ? expense.getCategory().trim() : "General");
        values.put("date", expense.getDate());
        values.put("recorded_by", expense.getRecordedBy() != null ? expense.getRecordedBy() : "");
        return db.insert(TABLE_EXPENSES, null, values);
    }

    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", expense.getDescription().trim());
        values.put("amount", expense.getAmount());
        values.put("category", expense.getCategory() != null && !expense.getCategory().trim().isEmpty()
                ? expense.getCategory().trim() : "General");
        values.put("date", expense.getDate());
        return db.update(TABLE_EXPENSES, values, "expense_id = ?", new String[]{String.valueOf(expense.getExpenseId())});
    }

    public void deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, "expense_id = ?", new String[]{String.valueOf(expenseId)});
    }

    public List<Expense> getAllExpenses() {
        return getExpensesFiltered("", "All");
    }

    public List<Expense> getExpensesFiltered(String keyword, String category) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_EXPENSES + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            sql.append(" AND LOWER(category) = ?");
            args.add(category.trim().toLowerCase(Locale.ROOT));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (description LIKE ? OR category LIKE ? OR date LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY date DESC, expense_id DESC");

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("expense_id"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int recIdx = cursor.getColumnIndex("recorded_by");
                String recordedBy = recIdx >= 0 ? cursor.getString(recIdx) : "";
                list.add(new Expense(id, desc, amount, cat, date, recordedBy));
            }
        }
        return list;
    }

    public List<String> getExpenseCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT DISTINCT category FROM " + TABLE_EXPENSES + " WHERE category IS NOT NULL AND TRIM(category) != '' ORDER BY category ASC",
                null)) {
            while (c.moveToNext()) {
                categories.add(c.getString(0));
            }
        }
        return categories;
    }

    public double getTotalExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM " + TABLE_EXPENSES, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        }
        return 0;
    }

    // =========================================================================
    // SALES & SALE ITEMS CRUD OPERATIONS (TRANSACTIONAL)
    // =========================================================================

    public boolean saveSale(Sale sale, List<SaleItem> items) {
        return saveSaleDetailed(sale, items).success;
    }

    public SaleSaveResult saveSaleDetailed(Sale sale, List<SaleItem> items) {
        if (sale == null || items == null || items.isEmpty()) {
            return new SaleSaveResult(false, -1, "Cart cannot be empty");
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Atomic stock & snapshot check before writing sale
            for (SaleItem item : items) {
                if (item.getQuantity() <= 0) {
                    return new SaleSaveResult(false, -1, "Invalid quantity for " + item.getProductName());
                }
                try (Cursor pc = db.rawQuery(
                        "SELECT name, cost_price, quantity, is_active FROM " + TABLE_PRODUCTS + " WHERE product_id = ?",
                        new String[]{String.valueOf(item.getProductId())})) {
                    if (!pc.moveToFirst() || pc.getInt(3) != 1) {
                        return new SaleSaveResult(false, -1, "Product no longer available: " + item.getProductName());
                    }
                    String dbName = pc.getString(0);
                    double dbCost = pc.getDouble(1);
                    int availableQty = pc.getInt(2);
                    if (availableQty < item.getQuantity()) {
                        return new SaleSaveResult(false, -1, "Insufficient stock for " + dbName + " (Available: " + availableQty + ")");
                    }
                    item.setProductName(dbName);
                    item.setCostPrice(dbCost);
                }
            }

            ContentValues saleValues = new ContentValues();
            saleValues.put("sale_date", sale.getSaleDate());
            saleValues.put("subtotal", sale.getSubtotal() > 0 ? sale.getSubtotal() : sale.getTotalAmount());
            saleValues.put("discount_percent", sale.getDiscountPercent());
            saleValues.put("total_amount", sale.getTotalAmount());
            saleValues.put("payment_method", sale.getPaymentMethod());
            if (sale.getCustomerId() > 0) {
                saleValues.put("customer_id", sale.getCustomerId());
            } else {
                saleValues.putNull("customer_id");
            }
            if (sale.getCashierUserId() > 0) {
                saleValues.put("cashier_user_id", sale.getCashierUserId());
            }
            saleValues.put("cashier_name", sale.getCashierName() != null ? sale.getCashierName() : "");

            long saleId = db.insert(TABLE_SALES, null, saleValues);
            if (saleId == -1) {
                return new SaleSaveResult(false, -1, "Failed to insert sale record");
            }

            for (SaleItem item : items) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("sale_id", saleId);
                itemValues.put("product_id", item.getProductId());
                itemValues.put("product_name", item.getProductName());
                itemValues.put("quantity", item.getQuantity());
                itemValues.put("unit_price", item.getUnitPrice());
                itemValues.put("cost_price", item.getCostPrice());
                db.insert(TABLE_SALE_ITEMS, null, itemValues);

                db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET quantity = quantity - ? WHERE product_id = ? AND quantity >= ?",
                        new Object[]{item.getQuantity(), item.getProductId(), item.getQuantity()});
            }

            db.setTransactionSuccessful();
            return new SaleSaveResult(true, saleId, null);
        } catch (Exception e) {
            return new SaleSaveResult(false, -1, "Transaction error: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    public List<Sale> getAllSales() {
        return getSalesFiltered("All", "", 0);
    }

    public List<Sale> getSalesFiltered(String paymentMethod, String keyword, int cashierUserId) {
        List<Sale> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT s.*, c.name as customer_name FROM " + TABLE_SALES
                + " s LEFT JOIN " + TABLE_CUSTOMERS + " c ON s.customer_id = c.customer_id WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (paymentMethod != null && !paymentMethod.trim().isEmpty() && !"All".equalsIgnoreCase(paymentMethod)) {
            sql.append(" AND LOWER(s.payment_method) = ?");
            args.add(paymentMethod.trim().toLowerCase(Locale.ROOT));
        }
        if (cashierUserId > 0) {
            sql.append(" AND s.cashier_user_id = ?");
            args.add(String.valueOf(cashierUserId));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (CAST(s.sale_id AS TEXT) LIKE ? OR c.name LIKE ? OR s.cashier_name LIKE ? OR s.sale_date LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY s.sale_date DESC, s.sale_id DESC");

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                list.add(mapSale(cursor));
            }
        }
        return list;
    }

    private Sale mapSale(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("sale_id"));
        String date = cursor.getString(cursor.getColumnIndexOrThrow("sale_date"));
        double subtotal = getOptionalDouble(cursor, "subtotal", 0.0);
        double discount = getOptionalDouble(cursor, "discount_percent", 0.0);
        double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
        String method = cursor.getString(cursor.getColumnIndexOrThrow("payment_method"));
        int custId = cursor.isNull(cursor.getColumnIndexOrThrow("customer_id"))
                ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow("customer_id"));
        String custName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
        int cashierId = getOptionalInt(cursor, "cashier_user_id", 0);
        String cashierName = getOptionalString(cursor, "cashier_name", "");

        if (subtotal == 0.0 && total > 0) {
            subtotal = total;
        }
        return new Sale(id, date, subtotal, discount, total, method, custId, custName, cashierId, cashierName);
    }

    public List<SaleItem> getSaleItems(int saleId) {
        List<SaleItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT si.*, COALESCE(si.product_name, p.name, 'Removed Product') AS resolved_name "
                + "FROM " + TABLE_SALE_ITEMS + " si "
                + "LEFT JOIN " + TABLE_PRODUCTS + " p ON si.product_id = p.product_id "
                + "WHERE si.sale_id = ? ORDER BY si.item_id ASC";
        try (Cursor c = db.rawQuery(query, new String[]{String.valueOf(saleId)})) {
            while (c.moveToNext()) {
                int itemId = c.getInt(c.getColumnIndexOrThrow("item_id"));
                int productId = c.isNull(c.getColumnIndexOrThrow("product_id"))
                        ? 0 : c.getInt(c.getColumnIndexOrThrow("product_id"));
                String prodName = c.getString(c.getColumnIndexOrThrow("resolved_name"));
                int qty = c.getInt(c.getColumnIndexOrThrow("quantity"));
                double unitPrice = c.getDouble(c.getColumnIndexOrThrow("unit_price"));
                double costPrice = getOptionalDouble(c, "cost_price", 0.0);

                SaleItem item = new SaleItem(itemId, saleId, productId, qty, unitPrice);
                item.setProductName(prodName);
                item.setCostPrice(costPrice);
                items.add(item);
            }
        }
        return items;
    }

    public boolean updateSale(int saleId, int customerId, String paymentMethod) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (customerId > 0) {
            cv.put("customer_id", customerId);
        } else {
            cv.putNull("customer_id");
        }
        cv.put("payment_method", paymentMethod);
        return db.update(TABLE_SALES, cv, "sale_id = ?", new String[]{String.valueOf(saleId)}) > 0;
    }

    /**
     * Voids/deletes a sale atomically and restores sold product quantities back into inventory.
     */
    public boolean deleteSale(int saleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery(
                    "SELECT product_id, quantity FROM " + TABLE_SALE_ITEMS + " WHERE sale_id = ?",
                    new String[]{String.valueOf(saleId)})) {
                while (c.moveToNext()) {
                    if (!c.isNull(0)) {
                        int productId = c.getInt(0);
                        int qty = c.getInt(1);
                        db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET quantity = quantity + ? WHERE product_id = ?",
                                new Object[]{qty, productId});
                    }
                }
            }
            db.delete(TABLE_SALE_ITEMS, "sale_id = ?", new String[]{String.valueOf(saleId)});
            int deleted = db.delete(TABLE_SALES, "sale_id = ?", new String[]{String.valueOf(saleId)});
            if (deleted > 0) {
                db.setTransactionSuccessful();
                return true;
            }
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // DASHBOARD & ANALYTICS AGGREGATES
    // =========================================================================

    public double getTotalSalesAmount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(SUM(total_amount), 0) FROM " + TABLE_SALES, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        }
        return 0;
    }

    public int getTodaySalesCountByCashier(int cashierUserId, String datePrefix) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SALES + " WHERE cashier_user_id = ? AND sale_date LIKE ?",
                new String[]{String.valueOf(cashierUserId), datePrefix + "%"})) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }

    public double getTodaySalesTotalByCashier(int cashierUserId, String datePrefix) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(total_amount), 0) FROM " + TABLE_SALES + " WHERE cashier_user_id = ? AND sale_date LIKE ?",
                new String[]{String.valueOf(cashierUserId), datePrefix + "%"})) {
            if (c.moveToFirst()) {
                return c.getDouble(0);
            }
        }
        return 0;
    }

    public int getTotalProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE is_active = 1", null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public int getTotalCustomersCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMERS, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public double getTotalCostOfGoodsSold() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COALESCE(SUM(si.quantity * COALESCE(si.cost_price, p.cost_price, 0)), 0) "
                + "FROM " + TABLE_SALE_ITEMS + " si LEFT JOIN " + TABLE_PRODUCTS + " p ON si.product_id = p.product_id";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        }
        return 0;
    }

    private double getOptionalDouble(Cursor c, String col, double fallback) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getDouble(idx) : fallback;
    }

    private int getOptionalInt(Cursor c, String col, int fallback) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getInt(idx) : fallback;
    }

    private String getOptionalString(Cursor c, String col, String fallback) {
        int idx = c.getColumnIndex(col);
        if (idx >= 0 && !c.isNull(idx)) {
            String val = c.getString(idx);
            return val != null ? val : fallback;
        }
        return fallback;
    }
}
