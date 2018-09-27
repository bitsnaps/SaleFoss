/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * <p/>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * <p/>
 * Created on 13/1/15 11:06 AM
 */
package com.odoo.addons.sale.models;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.ViewDebug;
import android.widget.Toast;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.addons.sale.Sales;
import com.odoo.base.addons.res.ResCompany;
import com.odoo.base.addons.res.ResCurrency;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.JSONUtils;
import com.odoo.core.utils.OResource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.lang.Thread.sleep;

public class SaleOrder extends OModel {
    public static final String TAG = SaleOrder.class.getSimpleName();
    //    public static final String AUTHORITY = "com.odoo.crm.provider.content.sync.sale_order";
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".provider.content.sync.sale_order";

    private static boolean isFirstUpdateProduct = false;
    public List<ODataRow> have_id_zero_records = null;
    private final Handler handler;
    OColumn invoice_status = new OColumn("Invoice Status", OVarchar.class).setDefaultValue("no");
    @Odoo.Functional(method = "getInvoiceStatusTitle", store = true, depends = {"invoice_status"})
    OColumn invoice_status_title = new OColumn("Invoice Title", OVarchar.class)
            .setLocalColumn();
    private Context mContext = getContext();
    private Context idContext = getContext();
    OColumn name = new OColumn(_s(R.string.field_label_name), OVarchar.class).setDefaultValue("offline");
    OColumn date_order = new OColumn(_s(R.string.field_label_date_order), ODateTime.class);
    @Odoo.onChange(method = "onPartnerIdChange", bg_process = true)
    OColumn partner_id = new OColumn(_s(R.string.field_label_partner_id), ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn user_id = new OColumn(_s(R.string.field_label_user_id), ResUsers.class, OColumn.RelationType.ManyToOne);
    OColumn amount_total = new OColumn(_s(R.string.field_label_amount_total), OFloat.class);
    OColumn payment_term_id = new OColumn(_s(R.string.field_label_payment_term), AccountPaymentTerm.class, OColumn.RelationType.ManyToOne);
    OColumn amount_untaxed = new OColumn(_s(R.string.field_label_amount_untaxed), OInteger.class);
    OColumn amount_tax = new OColumn(_s(R.string.field_label_amount_tax), OInteger.class);
    OColumn client_order_ref = new OColumn(_s(R.string.field_label_client_order_ref),
            OVarchar.class).setSize(100);
    OColumn state = new OColumn(_s(R.string.field_label_state), OVarchar.class).setSize(10)
            .setDefaultValue("draft");
    @Odoo.Functional(method = "getStateTitle", store = true, depends = {"state"})
    OColumn state_title = new OColumn(_s(R.string.field_label_state_title), OVarchar.class)
            .setLocalColumn();
    @Odoo.Functional(method = "storePartnerName", store = true, depends = {"partner_id"})
    OColumn partner_name = new OColumn(_s(R.string.field_label_partner_name), OVarchar.class)
            .setLocalColumn();
    OColumn currency_id = new OColumn(_s(R.string.field_label_currency_id), ResCurrency.class,
            OColumn.RelationType.ManyToOne);
    @Odoo.Functional(method = "storeCurrencySymbol", store = true, depends = {"currency_id"})
    OColumn currency_symbol = new OColumn(_s(R.string.field_label_currency_symbol), OVarchar.class)
            .setLocalColumn();
    OColumn order_line = new OColumn(_s(R.string.field_label_order_line), SalesOrderLine.class,
            OColumn.RelationType.OneToMany).setRelatedColumn("order_id");
    @Odoo.Functional(store = true, depends = {"order_line"}, method = "countOrderLines")
    OColumn order_line_count = new OColumn(_s(R.string.field_label_order_line_count), OVarchar.class).setLocalColumn();
    OColumn partner_invoice_id = new OColumn(_s(R.string.field_label_partner_invoice_id), OVarchar.class).setLocalColumn(); // Original
    OColumn partner_shipping_id = new OColumn(_s(R.string.field_label_partner_shipping_id), OVarchar.class).setLocalColumn(); // Original
    OColumn pricelist_id = new OColumn(_s(R.string.field_label_pricelist_id), OVarchar.class).setLocalColumn();
    OColumn fiscal_position = new OColumn(_s(R.string.field_label_fiscal_position), OVarchar.class).setLocalColumn();
    private int have_zero = 0;

    public SaleOrder(Context context, OUser user) {
        super(context, "sale.order", user);
        mContext = context;
        handler = new Handler(context.getMainLooper());
        setHasMailChatter(true);
    }

    public boolean setFirsLoadProduct(boolean isThere) {
        return this.isFirstUpdateProduct = isThere;
    }

    public boolean getFirsLoadProduct() {
        return this.isFirstUpdateProduct;
    }

    private String _s(int res_id) {
        return OResource.string(idContext, res_id);
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

    public ODataRow onPartnerIdChange(ODataRow row) {

        ODataRow data = new ODataRow();
        try {
            ResPartner partner = new ResPartner(mContext, getUser());
            AccountPaymentTerm term = new AccountPaymentTerm(mContext, getUser());
            ODataRow customer = partner.browse(row.getInt(OColumn.ROW_ID));
            data.put("partner_invoice_id", customer.get("partner_invoice_id"));
            data.put("partner_shipping_id", customer.get("partner_shipping_id"));
            data.put("pricelist_id", customer.get("pricelist_id"));
            data.put("payment_term_id", customer.get("payment_term_id"));
            data.put("fiscal_position", customer.get("fiscal_position"));

            partner.update(customer.getInt(OColumn.ROW_ID), data.toValues());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public ODataRow currency() {
        ResCompany company = new ResCompany(mContext, getUser());
        ODataRow row = company.browse(null, "id = ? ", new String[]{getUser().getCompanyId() + ""});
        if (row != null && !row.getString("currency_id").equals("false")) {
            return row.getM2ORecord("currency_id").browse();
        } else {
            ResCurrency currency = new ResCurrency(mContext, getUser());
            List<ODataRow> list = currency.select();
            if (list.size() > 0) {
                return list.get(0);
            }
        }
        return null;
    }

    public String getStateTitle(OValues row) {
        HashMap<String, String> mStates = new HashMap<String, String>();
        mStates.put("draft", mContext.getString(R.string.field_label_draft));
        mStates.put("sent", mContext.getString(R.string.field_label_sent));
        mStates.put("cancel", mContext.getString(R.string.field_label_canceled));
        mStates.put("waiting_date", mContext.getString(R.string.field_label_waiting_date));
        mStates.put("progress", mContext.getString(R.string.field_label_sale));
        mStates.put("sale", mContext.getString(R.string.field_label_sale));
        mStates.put("manual", mContext.getString(R.string.field_label_manual));
        mStates.put("shipping_except", mContext.getString(R.string.field_label_sipping_except));
        mStates.put("invoice_except", mContext.getString(R.string.field_label_invoice_except));
        mStates.put("done", mContext.getString(R.string.field_label_done));
        return mStates.get(row.getString("state"));
    }

    public String getInvoiceStatusTitle(OValues row) {
        HashMap<String, String> mStates = new HashMap<String, String>();
        mStates.put("upselling", mContext.getString(R.string.field_label_upselling));
        mStates.put("invoiced", mContext.getString(R.string.field_label_invoiced));
        mStates.put("to invoice", mContext.getString(R.string.field_label_to_invoice));
        mStates.put("no", mContext.getString(R.string.field_label_no));
        return mStates.get(row.getString("invoice_status"));
    }

    public String storeCurrencySymbol(OValues values) {
        try {
            if (!values.getString("currency_id").equals("false")) {
                JSONArray currency_id = new JSONArray(values.getString("currency_id"));
                return currency_id.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }

    public String storePartnerName(OValues values) {
        try {
            if (!values.getString("partner_id").equals("false")) {
                return ((ArrayList) values.get("partner_id")).get(1).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }

    public String countOrderLines(OValues values) {
        try {

            JSONArray order_line = new JSONArray(values.getString("order_line"));
            if (order_line.length() > 0) {
//                return " (" + order_line.length() + " " + mContext.getString(R.string.label_lines) + ")";
                return " (" + order_line.length() + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        return " " + mContext.getString(R.string.title_no_lines);
        return " (0)";
    }


    public void deleteOrder(final Sales.Type type, final ODataRow quotation, final OnOperationSuccessListener listener) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                SalesOrderLine lineOrder = new SalesOrderLine(getContext(), null);
                SaleOrder order = new SaleOrder(getContext(), null);

                try {
                    String sql = "SELECT _id FROM sale_order_line WHERE order_id = ?";
                    List<ODataRow> rec = lineOrder.query(sql,
                            new String[]{quotation.getInt(OColumn.ROW_ID).toString()});
                    for (ODataRow row : rec) {
                        lineOrder.delete(row.getInt(OColumn.ROW_ID));
                    }
                    order.delete(quotation.getInt(OColumn.ROW_ID));

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (listener != null) {
                    listener.OnSuccess();
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (listener != null) {
                    listener.OnCancelled();
                }
            }
        }.execute();
    }


    public void cancelOrder(final Sales.Type type, final ODataRow quotation, final OnOperationSuccessListener listener) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            private Boolean faultOrder = false;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_working));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                JSONObject mCancel;
                try {
                    OArguments args = new OArguments();
                    if (type == Sales.Type.SaleOrder) {
                        args.add(new JSONArray().put(quotation.getInt("id")));
                        args.add(new JSONObject());
                        mCancel = ((JSONObject) getServerDataHelper().callMethod("action_cancel", args));
                    } else {

                        mCancel = ((JSONObject) getServerDataHelper().callMethod("action_cancel", args));
                    }

                    OValues values = new OValues();
                    values.put("state", "cancel");
                    values.put("state_title", getStateTitle(values));
                    values.put("_is_dirty", "false");
                    update(quotation.getInt(OColumn.ROW_ID), values);
                } catch (Exception e) {
                    e.printStackTrace();
                    faultOrder = true;
                }
                faultOrder = false;
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.dismiss();
                if (!faultOrder)
                    if (listener != null) {
                        listener.OnSuccess();
                    } else if (listener != null) {
                        listener.OnCancelled();
                    }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                dialog.dismiss();
                if (listener != null) {
                    listener.OnCancelled();
                }
            }
        }.execute();
    }

    public void confirmSale(final ODataRow quotation, final OnOperationSuccessListener listener) {

        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            private Boolean faultOrder = false;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_please_wait_order));
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(100);
                dialog.setIndeterminate(true);

                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ODomain domain;
                    ODomain domainSaleOrder;
                    SalesOrderLine salesOrderLine = new SalesOrderLine(mContext, null); // getuser
                    SaleOrder salesOrder = new SaleOrder(mContext, null); // getuser
                    domain = new ODomain();
                    domain.add("id", "=", 0);
                    Log.e(TAG, "<< sale.order.line - syncing now >>");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("Creating record: " + quotation.getString("name"));
                        }
                    });

                    salesOrderLine.quickSyncRecords(domain);

                    int id = selectServerId(quotation.getInt(OColumn.ROW_ID));
                    if (id == 0) {
                        faultOrder = true;
                        return null;
                    }
                    quotation.put("id", id);
                    doOrderFullConfirm(quotation, dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                    faultOrder = true;
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

            @Override
            protected void onCancelled() {
                super.onCancelled();
                dialog.dismiss();
                if (listener != null) {
                    listener.OnCancelled();
                }
            }
        }.execute();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);

    }

    public void confirmAllSaleOrders(final List<ODataRow> quotation, final OnOperationSuccessListener listener) {

        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            private Boolean faultOrder = false;
            int countOrders = 0;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_loading));
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(quotation.size());
                dialog.setIndeterminate(true);

                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(600);
                    ODomain domainLine = new ODomain();
                    ODomain domainSale = new ODomain();
                    SalesOrderLine salesOrderLine = new SalesOrderLine(mContext, getUser());
                    SaleOrder sales = new SaleOrder(mContext, getUser());

                    sync().requestSync(SaleOrder.AUTHORITY);
//                    sync().wait();
                    int counter = 0;
                    String sql = "SELECT count(id) as counts FROM sale_order_line WHERE id = ?";
                    List<ODataRow> rec = salesOrderLine.query(sql, new String[]{"0"});
                    while (rec.get(0).getInt("counts") > 0 && counter < 50 ){
                        rec = salesOrderLine.query(sql, new String[]{"0"});
                        if (rec.get(0).getInt("counts") == 0)
                            break;
                        Thread. sleep(1500);
                        counter++;
                    }
//                    salesOrderLine.quickSyncRecords(domainLine);

//                    String sql = "SELECT id FROM sale_order_line WHERE id = ?";
//                    List<ODataRow> rec = salesOrderLine.query(sql, new String[]{"0"});
//                    if (rec.size() > 0){
//                        domainLine.add("id", "=", 0);
//                        Log.e(TAG, "<< sale.order.line - syncing now >>");
//                        salesOrderLine.quickSyncRecords(domainLine);
//                    }
//                    domainSale.addэтуже а("id", "=", "0");
//                    sales.quickSyncRecords(domainSale);

                    sales.doWorkflowFullConfirmEach(mContext, quotation, dialog);

                } catch (Exception e) {
                    e.printStackTrace();
                    faultOrder = true;
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


    public void newCopyQuotation(final ODataRow quotation, final OnOperationSuccessListener listener) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_working));
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    OArguments args = new OArguments();
                    args.add(new JSONArray().put(quotation.getInt("id")));
                    args.add(new JSONObject());
                    getServerDataHelper().callMethod("copy_quotation", args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dialog.dismiss();
                if (listener != null) {
                    listener.OnSuccess();
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                dialog.dismiss();
                if (listener != null) {
                    listener.OnCancelled();
                }
            }
        }.execute();
    }

    // New name fo Sale order Table
    public String newNameSaleOrder(String pream) {
        String nameOrder = "";
        String prefix = pream;

        SaleOrder sale = new SaleOrder(mContext, null);
        List<ODataRow> rows = sale.select(new String[]{"name"}, "name LIKE ?",
                new String[]{prefix + "%"});
        int i = 0;
        int[] numbersNames = new int[rows.size()];
        for (ODataRow row : rows) {
            nameOrder = row.getString("name");
            numbersNames[i] = Integer.parseInt(nameOrder.substring(nameOrder.indexOf(prefix) + prefix.length()));
            i++;
        }
        if (nameOrder != "") {
            Arrays.sort(numbersNames);
            nameOrder = Integer.toString(numbersNames[numbersNames.length - 1] + 1);
            if (nameOrder.length() == 1) {
                nameOrder = pream + "00" + nameOrder;
            } else if (nameOrder.length() == 2) {
                nameOrder = pream + "0" + nameOrder;
            } else if (nameOrder.length() >= 3) {
                nameOrder = pream + nameOrder;
            }
        } else {
            nameOrder = pream + "001";
        }
        return nameOrder;
    }

    public void syncReady(final Context context, final OnOperationSuccessListener listener) {
        new AsyncTask<Void, Void, Void>() {
            private Boolean faultOrder = false;
            private Object connect;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                OArguments args = new OArguments();
                args.add(new JSONObject());
                try {
                    connect = getServerDataHelper().callMethod("exist_db", args);
                    if (connect.equals(true)) {
                        Log.d(TAG, "exist_db returned TRUE ");
                    } else {
                        Log.d(TAG, "exist_db returned FALSE ");
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

    private void doOrderFullConfirm(final ODataRow quotation, final ProgressDialog dialog) {
        Object createInvoice = null;
        Object createDelivery = null;
        Object confirm = null;
        OArguments args = new OArguments();

        args.add(new JSONArray().put(quotation.getInt("id")));
        args.add(new JSONObject());
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setIndeterminate(false);
                    dialog.incrementProgressBy(30);

                }
            });

            confirm = getServerDataHelper().callMethod("action_confirm", args);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.incrementProgressBy(55);
                }
            });
            createDelivery = getServerDataHelper().callMethod("create_delivery", args);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.incrementProgressBy(85);
                }
            });
            createInvoice = getServerDataHelper().callMethod("create_invoice", args);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
                    .show();
        }

        if (confirm != null && confirm.equals(true)) {
            OValues values = new OValues();
            values.put("state", "sale");
            values.put("state_title", getStateTitle(values));
            if (createDelivery.equals(true) && createInvoice.equals(true)) {
                values.put("invoice_status", "invoiced");
                values.put("invoice_status_title", getInvoiceStatusTitle(values));
            }
            values.put("_is_dirty", "false");
            update(quotation.getInt(OColumn.ROW_ID), values);
        }
    }

    private void doWorkflowFullConfirm(Context context, final List<ODataRow> quotation) {
        Object confirm = null;
        Object createInvoice;
        Object createDelivery;
        Object confirm_full = null;

        if (checkNewQuotations(context) != null) {
            JSONArray idList = new JSONArray();
            OArguments args = new OArguments();
            for (final ODataRow qUpdate : quotation) {
                idList.put(selectServerId(qUpdate.getInt(OColumn.ROW_ID)));
            }
            args.add(idList);
            args.add(new JSONObject());

            try {
                confirm = getServerDataHelper().callMethod("action_confirm", args);
                confirm_full = getServerDataHelper().callMethod("create_with_full_confirm", args);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
                        .show();
            }

            if (confirm != null && confirm.equals(true)) {
                for (final ODataRow qUpdate : quotation) {
                    OValues values = new OValues();
                    values.put("state", "sale");
                    values.put("state_title", getStateTitle(values));
                    if (confirm_full.equals(true)) {
                        values.put("invoice_status", "invoiced");
                        values.put("invoice_status_title", getInvoiceStatusTitle(values));
                    }
                    values.put("_is_dirty", "false");
                    update(qUpdate.getInt(OColumn.ROW_ID), values);
                }
            } else {
                Toast.makeText(context, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
                        .show();
            }

        }
    }

    private void doWorkflowFullConfirmEach(Context context, final List<ODataRow> quotation, final ProgressDialog dialog) {
        Object confirm = null;
        Object createInvoice = null;
        Object createDelivery = null;
        int countOrders = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setIndeterminate(false);
            }
        });

        if (quotation.size() > 0 && quotation != null) {
            JSONArray idList = new JSONArray();

            for (final ODataRow qUpdate : quotation) {
                if (selectServerId(qUpdate.getInt(OColumn.ROW_ID)) == 0)
                    continue;
                OArguments args = new OArguments();
                args.add(new JSONArray().put(selectServerId(qUpdate.getInt(OColumn.ROW_ID))));
                args.add(new JSONObject());

                if (countOrders < dialog.getMax())
                    ++countOrders;
                dialog.setProgress(countOrders);

                try {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("Confirm: " + qUpdate.getString("name"));
                        }
                    });
                    confirm = getServerDataHelper().callMethod("action_confirm", args);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("Create delivery: " + qUpdate.getString("name"));
                        }
                    });
                    createDelivery = getServerDataHelper().callMethod("create_delivery", args);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage("Create invoice: " + qUpdate.getString("name"));
                        }
                    });
                    createInvoice = getServerDataHelper().callMethod("create_invoice", args);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
                            .show();
                }

                if (confirm != null && confirm.equals(true)) {
                    OValues values = new OValues();
                    values.put("state", "sale");
                    values.put("state_title", getStateTitle(values));
                    if (createDelivery.equals(true) && createInvoice.equals(true)) {
                        values.put("invoice_status", "invoiced");
                        values.put("invoice_status_title", getInvoiceStatusTitle(values));
                    }
                    values.put("_is_dirty", "false");
                    update(qUpdate.getInt(OColumn.ROW_ID), values);
                } else {
                    Toast.makeText(context, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    public List<ODataRow> checkNewQuotations(Context context) {
        boolean CheckOk = false;
        try {
            SaleOrder sale = new SaleOrder(context, null);
            String sql = "SELECT name, id, _id, state, partner_id, date_order  FROM sale_order WHERE (id = ? or state = ? ) and _is_active = ?";
            have_id_zero_records = sale.query(sql, new String[]{"0", "draft", "true"}); // crooked nail
            have_zero = have_id_zero_records.size();
            if (have_zero != 0) {
                CheckOk = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (CheckOk)
            return have_id_zero_records;
        return null;
    }


    //
    public JSONArray odooRecordsSend() {
        JSONArray ordersData = new JSONArray();
        try {
            SalesOrderLine salesOrderLine = new SalesOrderLine(mContext, null);
            ResPartner partner = new ResPartner(mContext, null);
            List<ODataRow> orders = checkNewQuotations(mContext);

            for (ODataRow row : orders) {
                JSONArray order_line = new JSONArray();
                List<ODataRow> order_lines = itemsOfOrderLines(row.getInt(OColumn.ROW_ID));

                for (ODataRow line : order_lines) {
                    JSONArray o_line = new JSONArray();
                    o_line.put(0);
                    o_line.put(0);
                    o_line.put(JSONUtils.toJSONObject(line));
                    order_line.put(o_line);
                }
                JSONObject data = new JSONObject();
                data.put("name", row.getString("name"));
                data.put("foss_mobile_id", row.getInt(OColumn.ROW_ID));
                data.put("partner_id", partner.selectServerId(row.getInt("partner_id")));
                data.put("user_id", getUser().getUserId());
                data.put("date_order", row.getString("date_order"));
//                data.put("payment_term", row.get("payment_term"));
                data.put("order_line", order_line);
//                data.put("picking_policy", "direct");
                //                return data;
                ordersData.put(data);
            }
            return ordersData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //
    public List<ODataRow> itemsOfOrderLines(int order_id) {

        List<ODataRow> items = new ArrayList<>();
        try {
            ProductProduct productProduct = new ProductProduct(mContext, getUser());
            SalesOrderLine salesOrderLine = new SalesOrderLine(mContext, getUser());

            String sql = "SELECT * FROM sale_order_line WHERE order_id = ? and _is_active = ?";
            List<ODataRow> oders = salesOrderLine.query(sql, new String[]{Integer.toString(order_id), "true"});

            for (ODataRow row : oders) {
                // Fill in row values for insert in the local DB
                OValues values = new OValues();
                values.put("product_id", productProduct.selectServerId(row.getInt("product_id")));
//            values.put("name", row.get("name")); // it is // mine
                values.put("product_uom_qty", row.getInt("product_uom_qty"));
//            values.put("product_uom", false);
//            values.put("price_unit", row.getFloat("price_unit"));
//            values.put("product_uos_qty", row.getInt("product_uom_qty"));
//            values.put("product_uos", false);
//            values.put("price_subtotal", row.getFloat("price_subtotal"));
//            values.put("default_code", row.get("default_code"));

                JSONArray tax_id = new JSONArray();
                tax_id.put(6);
                tax_id.put(false);
                tax_id.put(false);
//            values.put("tax_id", new JSONArray().put(tax_id));
//            values.put("th_weight", 0);
//            values.put("discount", 0);
//            values.put("order_id", order_id);
                items.add(values.toDataRow());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }


    public void saleRecordCreate(final OnOperationSuccessListener listener) {

        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            private Boolean faultOrder = false;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(mContext);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(mContext, R.string.title_please_wait_order));
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMax(100);
                dialog.setIndeterminate(true);

                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ODomain domain;
                    Object result;
                    SalesOrderLine salesOrderLine = new SalesOrderLine(mContext, getUser()); // getuser
                    SaleOrder salesOrder = new SaleOrder(mContext, getUser()); // getuser

                    domain = new ODomain();
                    domain.add("id", "=", 0);
                    Log.e(TAG, "<< sale.order.line - syncing now >>");

//                    salesOrderLine.quickSyncRecords(domain);
//                    salesOrder.quickSyncRecords(domain);

                    JSONArray newRecs = odooRecordsSend();
                    OArguments args = new OArguments();
                    args.add(newRecs);
                    args.add(new JSONObject());

//                    getServerDataHelper().callMethod("create_sale_orders_from_mobile", args);
                    result = getServerDataHelper().callMethod("sale.order", "create_sale_orders_from_mobile",
                            args, null, null);

                    Thread.sleep(600);
                } catch (Exception e) {
                    e.printStackTrace();
                    faultOrder = true;
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

            @Override
            protected void onCancelled() {
                super.onCancelled();
                dialog.dismiss();
                if (listener != null) {
                    listener.OnCancelled();
                }
            }
        }.execute();
    }

    public static interface OnOperationSuccessListener {

        public void OnSuccess();

        public void OnFault();

        public void OnCancelled();
    }

}

