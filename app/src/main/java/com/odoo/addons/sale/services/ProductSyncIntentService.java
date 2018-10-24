package com.odoo.addons.sale.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import com.odoo.addons.sale.models.ProductProduct;
import java.util.concurrent.TimeUnit;

public class ProductSyncIntentService extends IntentService {
    final String TAG = "ProductSync";
    private static boolean isFirstUpdateProduct = false;

    public ProductSyncIntentService() {
        super("Product Sync Service");
    }

    public static void setSyncToServer(boolean isThere) {
        ProductSyncIntentService.isFirstUpdateProduct = isThere;
    }

    public static boolean getSyncToServer() {
        return ProductSyncIntentService.isFirstUpdateProduct;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent START!");
        setSyncToServer(true);
        try {
            ProductProduct productProduct = new ProductProduct(getApplicationContext(), null);
            productProduct.syncProduct();
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onHandleIntent END!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setSyncToServer(false);
        Log.d(TAG, "DESTROY!");
    }
}
