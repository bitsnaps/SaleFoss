package com.odoo.addons.sale.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.odoo.addons.sale.models.ProductProduct;

import java.util.concurrent.TimeUnit;

public class SaleOrderSyncIntentService extends IntentService {
    final String TAG = "SaleOrderSync";
    private static boolean isFirstUpdateProduct = false;

    public static void setSyncToServer(boolean isThere) {
        SaleOrderSyncIntentService.isFirstUpdateProduct = isThere;
    }

    public static boolean getSyncToServer() {
        return SaleOrderSyncIntentService.isFirstUpdateProduct;
    }

    public SaleOrderSyncIntentService() {
        super("SaleOrder Sync Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent START!");
        setSyncToServer(true);
        try {
//
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
