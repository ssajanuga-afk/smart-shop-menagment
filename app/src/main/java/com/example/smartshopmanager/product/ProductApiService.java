package com.example.smartshopmanager.product;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartshopmanager.models.Product;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductApiService {
    private static final String URL = "https://dummyjson.com/products";
    private Context context;

    public interface ProductApiCallback {
        void onSuccess(List<Product> products);
        void onError(String message);
    }

    public ProductApiService(Context context) {
        this.context = context;
    }

    public void fetchApiProducts(ProductApiCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        List<Product> products = new ArrayList<>();
                        try {
                            JSONArray jsonArray = response.getJSONArray("products");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject obj = jsonArray.getJSONObject(i);
                                String title = obj.getString("title");
                                double price = obj.getDouble("price");
                                String category = obj.optString("category", "General");
                                String thumbnail = obj.optString("thumbnail", "");

                                Product product = new Product(title, price, category, thumbnail);
                                products.add(product);
                            }
                            callback.onSuccess(products);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onError("Parsing error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    }
                });

        queue.add(jsonObjectRequest);
    }
}
