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
//        sendNotification();
    }

    void sendNotification() {
        NotificationCompat.Builder builderNotif = new NotificationCompat.Builder(this);
        builderNotif.setSmallIcon(R.drawable.ic_action_sale_order);
        Intent intent = new Intent(this, OdooActivity.class);
        intent.putExtra("type", Sales.Type.SaleOrder);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builderNotif.setContentIntent(pIntent);
        builderNotif.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_foss));
        builderNotif.setContentTitle("Foss Sale");
        builderNotif.setContentText("Products sync successful!");
        builderNotif.setSubText("Tap to view application");

        builderNotif.mNotification.flags |= builderNotif.mNotification.FLAG_AUTO_CANCEL;
        builderNotif.mNotification.flags |= builderNotif.mNotification.DEFAULT_SOUND;
        builderNotif.mNotification.flags |= builderNotif.mNotification.DEFAULT_VIBRATE;

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, builderNotif.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setSyncToServer(false);
        Log.d(TAG, "DESTROY!");
    }
}
