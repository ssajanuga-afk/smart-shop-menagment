package com.example.smartshopmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartshopmanager.models.Category;
import com.example.smartshopmanager.models.Customer;
import com.example.smartshopmanager.models.Expense;
import com.example.smartshopmanager.models.Product;
import com.example.smartshopmanager.models.Sale;
import com.example.smartshopmanager.models.SaleItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SmartShop.db";
    public static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_CATEGORIES = "Categories";
    public static final String TABLE_PRODUCTS = "Products";
    public static final String TABLE_CUSTOMERS = "Customers";
    public static final String TABLE_SALES = "Sales";
    public static final String TABLE_SALE_ITEMS = "Sale_Items";
    public static final String TABLE_EXPENSES = "Expenses";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
                + "category_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category_name TEXT NOT NULL UNIQUE)";

        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + "("
                + "product_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "category_id INTEGER, "
                + "price REAL NOT NULL, "
                + "cost_price REAL DEFAULT 0, "
                + "quantity INTEGER DEFAULT 0, "
                + "FOREIGN KEY (category_id) REFERENCES " + TABLE_CATEGORIES + "(category_id))";

        String CREATE_CUSTOMERS_TABLE = "CREATE TABLE " + TABLE_CUSTOMERS + "("
                + "customer_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "email TEXT)";

        String CREATE_SALES_TABLE = "CREATE TABLE " + TABLE_SALES + "("
                + "sale_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_date TEXT NOT NULL, "
                + "total_amount REAL NOT NULL, "
                + "payment_method TEXT, "
                + "customer_id INTEGER, "
                + "FOREIGN KEY (customer_id) REFERENCES " + TABLE_CUSTOMERS + "(customer_id))";

        String CREATE_SALE_ITEMS_TABLE = "CREATE TABLE " + TABLE_SALE_ITEMS + "("
                + "item_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_id INTEGER NOT NULL, "
                + "product_id INTEGER NOT NULL, "
                + "quantity INTEGER NOT NULL, "
                + "unit_price REAL NOT NULL, "
                + "FOREIGN KEY (sale_id) REFERENCES " + TABLE_SALES + "(sale_id), "
                + "FOREIGN KEY (product_id) REFERENCES " + TABLE_PRODUCTS + "(product_id))";

        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + "expense_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "description TEXT NOT NULL, "
                + "amount REAL NOT NULL, "
                + "category TEXT, "
                + "date TEXT NOT NULL)";

        db.execSQL(CREATE_CATEGORIES_TABLE);
        db.execSQL(CREATE_PRODUCTS_TABLE);
        db.execSQL(CREATE_CUSTOMERS_TABLE);
        db.execSQL(CREATE_SALES_TABLE);
        db.execSQL(CREATE_SALE_ITEMS_TABLE);
        db.execSQL(CREATE_EXPENSES_TABLE);

        // Insert default category
        ContentValues cv = new ContentValues();
        cv.put("category_name", "General");
        db.insert(TABLE_CATEGORIES, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }

    // --- CATEGORY OPERATIONS ---
    public long addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("category_name", name);
        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
                list.add(new Category(id, name));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- PRODUCT OPERATIONS ---
    public long addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", product.getName());
        values.put("category_id", product.getCategoryId());
        values.put("price", product.getPrice());
        values.put("cost_price", product.getCostPrice());
        values.put("quantity", product.getQuantity());
        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return id;
    }

    public int updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", product.getName());
        values.put("category_id", product.getCategoryId());
        values.put("price", product.getPrice());
        values.put("cost_price", product.getCostPrice());
        values.put("quantity", product.getQuantity());
        int rows = db.update(TABLE_PRODUCTS, values, "product_id = ?", new String[]{String.valueOf(product.getProductId())});
        db.close();
        return rows;
    }

    public void deleteProduct(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRODUCTS, "product_id = ?", new String[]{String.valueOf(productId)});
        db.close();
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, c.category_name FROM " + TABLE_PRODUCTS + " p LEFT JOIN " + TABLE_CATEGORIES + " c ON p.category_id = c.category_id";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("product_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int catId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                double cost = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                String catName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));

                Product p = new Product(id, name, catId, price, cost, qty);
                p.setCategoryName(catName);
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, c.category_name FROM " + TABLE_PRODUCTS + " p LEFT JOIN " + TABLE_CATEGORIES + " c ON p.category_id = c.category_id WHERE p.name LIKE ?";
        Cursor cursor = db.rawQuery(query, new String[]{"%" + keyword + "%"});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("product_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int catId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                double cost = cursor.getDouble(cursor.getColumnIndexOrThrow("cost_price"));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                String catName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));

                Product p = new Product(id, name, catId, price, cost, qty);
                p.setCategoryName(catName);
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- CUSTOMER OPERATIONS ---
    public long addCustomer(Customer customer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", customer.getName());
        values.put("phone", customer.getPhone());
        values.put("email", customer.getEmail());
        long id = db.insert(TABLE_CUSTOMERS, null, values);
        db.close();
        return id;
    }

    public int updateCustomer(Customer customer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", customer.getName());
        values.put("phone", customer.getPhone());
        values.put("email", customer.getEmail());
        int rows = db.update(TABLE_CUSTOMERS, values, "customer_id = ?", new String[]{String.valueOf(customer.getCustomerId())});
        db.close();
        return rows;
    }

    public void deleteCustomer(int customerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CUSTOMERS, "customer_id = ?", new String[]{String.valueOf(customerId)});
        db.close();
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CUSTOMERS, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("customer_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                list.add(new Customer(id, name, phone, email));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Customer> searchCustomers(String keyword) {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CUSTOMERS + " WHERE name LIKE ? OR phone LIKE ?", new String[]{"%" + keyword + "%", "%" + keyword + "%"});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("customer_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                list.add(new Customer(id, name, phone, email));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- EXPENSE OPERATIONS ---
    public long addExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", expense.getDescription());
        values.put("amount", expense.getAmount());
        values.put("category", expense.getCategory());
        values.put("date", expense.getDate());
        long id = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return id;
    }

    public int updateExpense(Expense expense) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", expense.getDescription());
        values.put("amount", expense.getAmount());
        values.put("category", expense.getCategory());
        values.put("date", expense.getDate());
        int rows = db.update(TABLE_EXPENSES, values, "expense_id = ?", new String[]{String.valueOf(expense.getExpenseId())});
        db.close();
        return rows;
    }

    public void deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, "expense_id = ?", new String[]{String.valueOf(expenseId)});
        db.close();
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES + " ORDER BY date DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("expense_id"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                list.add(new Expense(id, desc, amount, cat, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public double getTotalExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_EXPENSES, null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    // --- SALES OPERATIONS (TRANSACTION) ---
    public boolean saveSale(Sale sale, List<SaleItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues saleValues = new ContentValues();
            saleValues.put("sale_date", sale.getSaleDate());
            saleValues.put("total_amount", sale.getTotalAmount());
            saleValues.put("payment_method", sale.getPaymentMethod());
            if (sale.getCustomerId() > 0) {
                saleValues.put("customer_id", sale.getCustomerId());
            }
            long saleId = db.insert(TABLE_SALES, null, saleValues);
            if (saleId == -1) {
                return false;
            }

            for (SaleItem item : items) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("sale_id", saleId);
                itemValues.put("product_id", item.getProductId());
                itemValues.put("quantity", item.getQuantity());
                itemValues.put("unit_price", item.getUnitPrice());
                db.insert(TABLE_SALE_ITEMS, null, itemValues);

                // Update product stock
                db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET quantity = quantity - ? WHERE product_id = ?",
                        new Object[]{item.getQuantity(), item.getProductId()});
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public List<Sale> getAllSales() {
        List<Sale> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT s.*, c.name as customer_name FROM " + TABLE_SALES + " s LEFT JOIN " + TABLE_CUSTOMERS + " c ON s.customer_id = c.customer_id ORDER BY s.sale_date DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("sale_id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("sale_date"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
                String method = cursor.getString(cursor.getColumnIndexOrThrow("payment_method"));
                int custId = cursor.getInt(cursor.getColumnIndexOrThrow("customer_id"));
                String custName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));

                list.add(new Sale(id, date, total, method, custId, custName));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public double getTotalSalesAmount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total_amount) FROM " + TABLE_SALES, null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public int getTotalProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalCustomersCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMERS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public double getTotalCostOfGoodsSold() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(si.quantity * p.cost_price) FROM " + TABLE_SALE_ITEMS + " si JOIN " + TABLE_PRODUCTS + " p ON si.product_id = p.product_id";
        Cursor cursor = db.rawQuery(query, null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }
}
