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
 * Created on 13/1/15 11:11 AM
 */
package com.odoo.addons.sale.models;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductProduct extends OModel {
    public static final String TAG = ProductProduct.class.getSimpleName();
    //    public static final String AUTHORITY = "com.odoo.crm.provider.content.sync.product_product";
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".provider.content.sync.product_product";

    private Context idContext = getContext();
    private final Handler handler;

    OColumn product_tmpl_id = new OColumn(_s(R.string.field_label_product_tmpl_id), ProductTemplate.class,
            OColumn.RelationType.ManyToOne);
    @Odoo.Functional(method = "storeProductName", store = true, depends = {"product_tmpl_id"})
    OColumn name_template = new OColumn(_s(R.string.field_label_name), OVarchar.class).setSize(128).setLocalColumn();
    OColumn default_code = new OColumn(_s(R.string.field_label_default_code), OVarchar.class);
    OColumn lst_price = new OColumn(_s(R.string.field_label_lst_price), OFloat.class);
    OColumn sale_ok = new OColumn(_s(R.string.field_label_sale_ok), OBoolean.class).setDefaultValue(false);

    public ProductProduct(Context context, OUser user) {
        super(context, "product.product", user);
        handler = new Handler(context.getMainLooper());
        setDefaultNameColumn("name_template");
    }

    private String _s(int res_id) {
        return OResource.string(idContext, res_id);
    }

    public String storeProductName(OValues values) {
        try {
            if (!values.getString("product_tmpl_id").equals("false")) {
                return ((ArrayList) values.get("product_tmpl_id")).get(1).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }

    @Override
    public ODomain defaultDomain() {
        ODomain domain = new ODomain();
        return domain;
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public void syncProduct(final Context context, final ProductProduct.OnOperationSuccessListener listener) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            private Boolean faultOrder = false;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(context);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(context, R.string.title_loading_product));
                dialog.setCancelable(false); // original false
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                ODomain domain = new ODomain();
                OArguments args = new OArguments();
                args.add(new JSONObject());
                final int items;

                try {
                    Thread.sleep(300);
                    ProductTemplate productTemplate = new ProductTemplate(getContext(), getUser());
                    List<Object> maxDate = new ArrayList<>();
                    String sql = "SELECT max(write_date) as maxDate FROM product_template";
                    List<ODataRow> records = productTemplate.query(sql);
                    for (ODataRow row : records) {
                        maxDate.add(row.get("maxDate"));
                    }
                    ODomain domainDate = new ODomain();
                    domainDate.add("write_date", ">", maxDate.get(0));
                    List<Integer> newIds = new ArrayList<>();
                    OdooFields fields = new OdooFields(new String[]{"id, write_date"});
                    List<ODataRow> dates = productTemplate.getServerDataHelper().searchRecords(fields, domainDate, 10);
                    for (ODataRow row : dates) {
                        newIds.add(((Double) row.get("id")).intValue());
                    }
                    items = newIds.size();
                    if (items > 0) {
                        domain.add("product_tmpl_id", "in", newIds);
                        if (items > 1)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.setMessage("Updating: " + ((Integer) items).toString() + " items");
                                }
                            });
                    }
                    Object checkConnect = getServerDataHelper().callMethod("exist_db", args);
                    if (checkConnect != null) {
                        quickSyncRecords(domain);
                    }
                } catch (Exception e) {
                    faultOrder = true;
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.dismiss();
                if (listener != null) {
                    if (!faultOrder) {
                        listener.OnSuccess();
                    } else {
                        listener.OnFault();
                    }
                }

            }
        }.execute();
    }

    public void syncProductNew(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(300);
                    ODomain domain = new ODomain();
                    ProductProduct product = new ProductProduct(context, null);
                    domain.add("id", "not in", product.getServerIds());
                    product.quickSyncRecords(domain);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public interface OnOperationSuccessListener {
        void OnSuccess();

        void OnFault();

        void OnCancelled();
    }

}


