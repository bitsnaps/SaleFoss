/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p>
 * Created on 9/1/15 11:32 AM
 */
package com.odoo;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.odoo.addons.sale.Sales;
import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.addons.sale.models.SaleOrder;
import com.odoo.core.account.About;
import com.odoo.core.account.OdooLogin;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.support.OUser;
import com.odoo.core.support.sync.SyncUtils;
import com.odoo.core.utils.OAppBarUtils;
import com.odoo.core.utils.OPreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();
    public static final String ACTION_ABOUT = "com.odoo.ACTION_ABOUT";
    public static final String ACTION_ORDER_SYNCHRONIZATION = "com.odoo.ACTION_ORDER_SYNCHRONIZATION";
    public static final String ACTION_PRODUCT_SYNCHRONIZATION = "com.odoo.ACTION_PRODUCT_SYNCHRONIZATION";

    ProductProduct.OnOperationSuccessListener confirmProduct = new ProductProduct.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            App mContext = (App) getApplicationContext();
            Toast.makeText(mContext, R.string.toast_recs_updated, Toast.LENGTH_LONG).show();
        }

        @Override
        public void OnFault() {
            App mContext = (App) getApplicationContext();
            Toast.makeText(mContext, R.string.label_product_download_fault, Toast.LENGTH_LONG).show();
        }

        @Override
        public void OnCancelled() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_setting_activity);
        OAppBarUtils.setAppBar(this, true);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setHomeButtonEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(R.string.title_application_settings);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        Boolean checkConnection = true;
        if (intent.getAction() != null
                && intent.getAction().equals(ACTION_ABOUT)) {
            Intent about = new Intent(this, About.class);
            super.startActivity(about);
            return;
        }
        if (intent.getAction() != null) {
            SaleOrder salesOrders = new SaleOrder(this, null);
            ProductProduct products = new ProductProduct(this, null);
            App app = (App) this.getApplicationContext();
            if (app.inNetwork()) {
                if (intent.getAction().equals(ACTION_ORDER_SYNCHRONIZATION)) {
                    updateOrders(salesOrders);
                    return;
                }
                if (intent.getAction().equals(ACTION_PRODUCT_SYNCHRONIZATION)) {
                    updateProducts(products);
                    return;
                }
            } else {
                checkConnection = false;
                Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
            }
        }
        if (checkConnection)
            super.startActivity(intent);
    }

    private void updateOrders(final SaleOrder sales) {
        final List<ODataRow> have_id_zero_records = sales.checkNewQuotations(this);
        if (have_id_zero_records != null) {
            if (!SaleOrder.getSyncToServer()) {
                sales.confirmAllOrders(have_id_zero_records);
                Toast.makeText(getApplicationContext(), R.string.toast_process_started, Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(getApplicationContext(), R.string.toast_process_started_already, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.toast_no_new_records, Toast.LENGTH_LONG).show();
        }
        sales.syncReady();
    }

    private void updateProducts(ProductProduct product) {
        product.syncProduct(this, confirmProduct);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        settingUpdated();
    }

    private void settingUpdated() {
        OUser user = OUser.current(this);
        if (user == null) {
            Intent loginActivity = new Intent(this, OdooLogin.class);
            loginActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginActivity);
            finish();
        } else {
            Account mAccount = user.getAccount();
            OPreferenceManager mPref = new OPreferenceManager(this);
            int sync_interval = mPref.getInt("sync_interval", 1440);

            List<String> default_authorities = new ArrayList<>();
//            default_authorities.add("com.android.calendar");
//            default_authorities.add("com.android.contacts");
            SyncAdapterType[] list = ContentResolver.getSyncAdapterTypes();
            for (SyncAdapterType lst : list) {
                if (lst.authority.contains("com.odoo")
                        && lst.authority.contains("providers")) {
                    default_authorities.add(lst.authority);
                }
            }
            for (String authority : default_authorities) {
                boolean isSyncActive = ContentResolver.getSyncAutomatically(
                        mAccount, authority);
                if (isSyncActive) {
                    SyncUtils.get(this).setSyncPeriodic(authority, sync_interval, 60, 1);
                }
            }
//            Toast.makeText(this, OResource.string(this, R.string.toast_setting_saved),
//                    Toast.LENGTH_LONG).show();
        }
    }

}
