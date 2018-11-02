package com.odoo.addons.sale.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.odoo.OdooActivity;
import com.odoo.R;
import com.odoo.addons.sale.Sales;
import com.odoo.addons.sale.models.ProductProduct;
import java.util.concurrent.TimeUnit;

public class ProductSyncIntentService extends IntentService {
    private NotificationManager nm;
    private static Boolean checkToastsActive = false;

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

    static public boolean checkToastsActive(Boolean check) {
        return checkToastsActive = check;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Product onHandleIntent START!");
        setSyncToServer(true);
        try {
            ProductProduct productProduct = new ProductProduct(getApplicationContext(), null);
            productProduct.syncProduct(checkToastsActive);
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Product onHandleIntent END!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setSyncToServer(false);
        Log.d(TAG, "DESTROY!");
    }
}
