package com.odoo.addons.sale.services;

import android.app.IntentService;
import android.app.Notification;
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
import com.odoo.addons.sale.models.SaleOrder;

import java.util.concurrent.TimeUnit;

public class SaleOrderSyncIntentService extends IntentService {

    final String TAG = "SaleOrderSync";
    private static boolean isFirstUpdateProduct = false;
    private NotificationManager nm;
    public static final int SYNC_ONLY = 1;
    public static final int SYNC_AND_CONFIRM = 2;

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
        int syncType = intent.getIntExtra("syncType", 0);
        setSyncToServer(true);
        try {
            switch (syncType) {
                case SYNC_ONLY:
                    Log.d(TAG, "SYNC STARED");
                    new SaleOrder(getApplication(), null ).syncSaleOrder();
                    break;
                case SYNC_AND_CONFIRM:
                    Log.d(TAG, "SYNC AND CONFIRM STARED");
                    new SaleOrder(getApplication(), null ).syncAndBackupConfirm();
                    sendNotification();
                    break;
            }

            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "onHandleIntent END!");
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
        builderNotif.setContentText("Orders sync sync successful!");
        builderNotif.setSubText("Tap to view application");
        builderNotif.mNotification.flags |= builderNotif.mNotification.FLAG_AUTO_CANCEL;
        builderNotif.setDefaults(Notification.DEFAULT_SOUND);

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
