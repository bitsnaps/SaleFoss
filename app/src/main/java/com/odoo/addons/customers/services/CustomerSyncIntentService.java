package com.odoo.addons.customers.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.rpc.helper.ODomain;

import java.util.concurrent.TimeUnit;

public class CustomerSyncIntentService extends IntentService {
    private static boolean isFirstUpdateCustomer = false;

    final String TAG = "CustomerSync";

    public CustomerSyncIntentService() {
        super("Customers Sync Service");
    }

    public static void setSyncToServer(boolean isThere) {
        CustomerSyncIntentService.isFirstUpdateCustomer = isThere;
    }
    public static boolean getSyncToServer() {
        return CustomerSyncIntentService.isFirstUpdateCustomer;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "START!");
        setSyncToServer(true);
        try {
            ODomain domain = new ODomain();
            ResPartner resPartner = new ResPartner(getApplicationContext(), null);
            resPartner.quickSyncRecords(domain);
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setSyncToServer(false);
        Log.d(TAG, "DESTROY!");
    }

}
