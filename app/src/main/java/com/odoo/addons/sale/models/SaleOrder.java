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
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.addons.customers.Customers;
import com.odoo.addons.sale.Sales;
import com.odoo.addons.sale.services.SaleOrderSyncIntentService;
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
import com.odoo.core.rpc.handler.OdooVersionException;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.rpc.listeners.IOdooConnectionListener;
import com.odoo.core.rpc.listeners.OdooError;
import com.odoo.core.service.ISyncServiceListener;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OPreferenceManager;
import com.odoo.core.utils.OResource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SaleOrder extends OModel implements IOdooConnectionListener, ISyncServiceListener {
    public static final String TAG = SaleOrder.class.getSimpleName();
    //    public static final String AUTHORITY = "com.odoo.crm.provider.content.sync.sale_order";
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID +
            ".provider.content.sync.sale_order";

    private static boolean isFirstUpdateProduct = false;
    private static List<ODataRow> listIds = null;
    public List<ODataRow> have_id_zero_records = null;
    private final Handler handler;
    private Context mContext;
    private Context idContext = getContext();

    public enum Type {
        NoRecords, SyncFailed, SyncTimedOut, SyncSuccess
    }

    public static Type mType = Type.NoRecords;


    OColumn invoice_status = new OColumn("Invoice Status", OVarchar.class).setDefaultValue("no");
    @Odoo.Functional(method = "getInvoiceStatusTitle", store = true, depends = {"invoice_status"})
    OColumn invoice_status_title = new OColumn("Invoice Title", OVarchar.class)
            .setLocalColumn();
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
            .setDefaultValue("no");
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
    OColumn _is_local_only = new OColumn("Local Status", OVarchar.class).setSize(10)
            .setDefaultValue("oldsync").setLocalColumn();

    private int have_zero = 0;

    public SaleOrder(Context context, OUser user) {
        super(context, "sale.order", user);
        mContext = context;
        handler = new Handler(context.getMainLooper());
//        setHasMailChatter(true);
    }

    public static void setSyncToServer(boolean isThere) {
        SaleOrder.isFirstUpdateProduct = isThere;
    }

    public static boolean getSyncToServer() {
        return SaleOrder.isFirstUpdateProduct;
    }

    private static List<ODataRow> getSyncToServerIds() {
        return SaleOrder.listIds;
    }

    private String _s(int res_id) {
        return OResource.string(idContext, res_id);
    }

    @Override
    public void onSyncStarted() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(mContext, R.string.toast_process_started, Toast.LENGTH_SHORT).show();
//                }
//            });
    }

    @Override
    public void onSyncTimedOut() {
        this.mType = Type.SyncTimedOut;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(mContext, R.string.toast_problem_with_sync, Toast.LENGTH_LONG)
//                            .show();
//                }
//            });
    }

    @Override
    public void onSyncFailed() {
        this.mType = Type.SyncFailed;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(mContext, R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
//                            .show();
//                }
//            });
    }

    public void onSyncFinished() {
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
        mStates.put("no", mContext.getString(R.string.field_label_draft_no));
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
                return " (" + order_line.length() + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                Object mDelete;
                OArguments args = new OArguments();

                try {
                    String sql = "SELECT _id FROM sale_order_line WHERE order_id = ?";
                    List<ODataRow> rec = lineOrder.query(sql,
                            new String[]{quotation.getInt(OColumn.ROW_ID).toString()});
                    for (ODataRow row : rec) {
                        lineOrder.delete(row.getInt(OColumn.ROW_ID));
                    }
                    args.add(new JSONArray().put(quotation.getInt("id")));
                    args.add(new JSONObject());
                    if (quotation.getInt("id") != 0)
                        mDelete = getServerDataHelper().callMethod("delete_order", args);
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

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    // ----------------------------------------------------------------------------------------------------
    public void syncSaleOrder() {
        OPreferenceManager preferenceManager = new OPreferenceManager(mContext);
        ODomain domain = new ODomain();
        List<Integer> serverIds = getServerIds();
        if (serverIds.size() > 0) {
            domain.add("&");
        }
        int data_limit = preferenceManager.getInt("sync_data_limit", 2);
        domain.add("create_date", ">=", ODateUtils.getDateBefore(data_limit));
        if (serverIds.size() > 0) {
            domain.add("id", "not in", serverIds);
        }
        quickSyncRecords(domain);
    }

    public Type syncAndBackupConfirm() {
        final List<ODataRow> quotation = checkNewQuotations(mContext);
/**
 Move to Sale Order dir
 */
        this.mType = Type.NoRecords;
        if (quotation != null) {
            for (final ODataRow qUpdate : quotation) {
                OValues values = new OValues();
                values.put("state", "draft");
                values.put("_is_local_only", "sync");
                update(qUpdate.getInt(OColumn.ROW_ID), values);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_no_recs_sync, Toast.LENGTH_SHORT)
                            .show();
                }
            });
            return Type.NoRecords;
        }
/**
 Sync all Orders
 */
        Log.d("quickSyncRecords: ", "TRUE");
        try {
            quickSyncRecords(new ODomain().add("id", "=", 0));
/**
 Orders Validate
 */
            ValidateOrder(quotation);

        } catch (Exception e) {
            Log.d("Sync:", "Bad connect!");
            return Type.SyncTimedOut;
        }
/**
 Orders Confirming block
 */
        if (mType == Type.SyncTimedOut)
            return Type.SyncTimedOut;
        if (mType == Type.SyncFailed)
            return Type.SyncFailed;

        SalesOrderLine lines = new SalesOrderLine(mContext, getUser());
        String sql = "SELECT distinct order_id FROM sale_order_line WHERE id = ? and _is_active = ?";
        List<ODataRow> linesIds = lines.query(sql, new String[]{"0", "true"});
        if (quotation != null && linesIds.size() == 0) {

            if (ValidateOrder(quotation)) {
                Log.d("doWorkflowFull: ", "TRUE");
                doWorkflowFullConfirmEach(mContext, quotation, null);
                Log.d("doWorkflowFull: : ", "FALSE");
            }
        }
/**
 End Sync module
 */
        Log.d("else doWorkflowFull: : ", "FALSE");
        return mType;
    }

    private Boolean ValidateOrder(List<ODataRow> quotation) {
        Object confirmDelete = null;
        SalesOrderLine lines = new SalesOrderLine(mContext, getUser());
        List<Integer> serverIds = new ArrayList<>(); // if QuickSyncRecord
        List<Integer> localIds = new ArrayList<>();
        if (quotation == null) {
            return false;
        }
        try {
            for (ODataRow row : quotation) {
                localIds.add(row.getInt(OColumn.ROW_ID));
                if (selectServerId(row.getInt(OColumn.ROW_ID)) == 0)
                    continue;
                serverIds.add(selectServerId(row.getInt(OColumn.ROW_ID)));
            }
            List<Integer> idLocalOrders = new ArrayList<>();
            String sql = "SELECT distinct name, id, _id, order_id FROM sale_order_line WHERE order_id IN (" +
                    TextUtils.join(",", localIds) + ")";
            List<ODataRow> linesIds = lines.query(sql);

            if (linesIds.size() > 0) {
                for (ODataRow row : linesIds) {
                    idLocalOrders.add(row.getInt("id"));
                }
                List<Integer> idServerOrders = new ArrayList<>();
                OdooFields fields = new OdooFields(new String[]{"id"});
                ODomain domain = new ODomain();
                domain.add("order_id", "in", serverIds);
                List<ODataRow> records = lines.getServerDataHelper().searchRecords(fields, domain, 40);
                for (ODataRow row : records) {
                    idServerOrders.add(((Double) row.get("id")).intValue());
                }

                idServerOrders.removeAll(idLocalOrders);
                if (idServerOrders.size() > 0) {
                    JSONArray serverIdsJSON = new JSONArray(); // if call server
                    for (int idRec : idServerOrders) {
                        serverIdsJSON.put(idRec);
                    }

                    if (serverIdsJSON.length() > 0) {
                        OArguments args = new OArguments();
                        args.add(serverIdsJSON);
                        args.add(new JSONObject());
                        confirmDelete = lines.getServerDataHelper().callMethod("delete_order_line", args);
                    }
                }
            }
/**
 *  Check order to double an delete from server if find such.
 */
            List<String> namesOrders = new ArrayList<>();
            List<Integer> idServerOrderIds = new ArrayList<>();
            JSONArray serverIdsJSON = new JSONArray(); // if call server

            sql = "SELECT name, id FROM sale_order WHERE id in (" +
                    TextUtils.join(",", serverIds) + ")";
            linesIds = query(sql);
            for (ODataRow row : linesIds) {
                namesOrders.add(row.getString("name"));
            }

            OdooFields fields = new OdooFields(new String[]{"id"});
            ODomain domain = new ODomain();
            domain.add("name", "in", namesOrders);
            List<ODataRow> records = getServerDataHelper().searchRecords(fields, domain, 40);
            for (ODataRow row : records) {
                idServerOrderIds.add(((Double) row.get("id")).intValue());
            }
            idServerOrderIds.removeAll(serverIds);

            for (int idRec : idServerOrderIds) {
                serverIdsJSON.put(idRec);
            }

            if (serverIdsJSON.length() > 0) {
                OArguments args = new OArguments();
                args.add(serverIdsJSON);
                args.add(new JSONObject());
                getServerDataHelper().callMethod("delete_order", args);
            }

        } catch (Exception e) {
            Log.d("ValidateOrder", "Bad connect!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_problem_with_sync, Toast.LENGTH_LONG)
                            .show();
                }
            });
            SaleOrder.mType = Type.SyncFailed;
            return false;
        }
        if (serverIds.size() > 0) {
            quickSyncRecords(new ODomain().add("id", "in", serverIds));
        }

        return true;
    }
    // ----------------------------------------------------------------------------------------------------

    // New name fo Sale order Table
    public String newNameSaleOrder(String pream, String postfix) {
        String nameOrder = "";
        String prefix = pream;

        SaleOrder sale = new SaleOrder(mContext, null);
        List<ODataRow> rows = sale.select(new String[]{"name"}, "name LIKE ?",
                new String[]{prefix + "%" + postfix});
        int i = 0;
        int[] numbersNames = new int[rows.size()];
        for (ODataRow row : rows) {
            nameOrder = row.getString("name");
            String nameWitoutPrefix = nameOrder.substring(nameOrder.indexOf(prefix) + prefix.length());
            numbersNames[i] = Integer.parseInt(nameWitoutPrefix.substring(0, nameWitoutPrefix.indexOf(postfix)));
            i++;
        }
        if (nameOrder != "") {
            Arrays.sort(numbersNames);
            nameOrder = Integer.toString(numbersNames[numbersNames.length - 1] + 1);
            if (nameOrder.length() == 1) {
                nameOrder = pream + "00" + nameOrder + postfix;
            } else if (nameOrder.length() == 2) {
                nameOrder = pream + "0" + nameOrder + postfix;
            } else if (nameOrder.length() >= 3) {
                nameOrder = pream + nameOrder + postfix;
            }
        } else {
            nameOrder = pream + "001" + postfix;
        }
        return nameOrder;
    }

    public void syncReady() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    com.odoo.core.rpc.Odoo.createInstance(getContext(), getUser().getHost()).setOnConnect(SaleOrder.this);
                } catch (OdooVersionException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void doWorkflowFullConfirmEach(Context context, final List<ODataRow> quotation, final ProgressDialog dialog) {
        Object confirm;
        Object createInvoice;
        Object createDelivery;
        int countOrders = 0;
        SaleOrder.setSyncToServer(true);

        if (dialog != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.setIndeterminate(false);
                }
            });
        }

        if (quotation.size() > 0 && quotation != null) {
            for (final ODataRow qUpdate : quotation) {
                OValues values = new OValues();
                values.put("invoice_status", "no");
                values.put("invoice_status_title", getInvoiceStatusTitle(values));
                update(qUpdate.getInt(OColumn.ROW_ID), values);
            }

            for (final ODataRow qUpdate : quotation) {
                if (selectServerId(qUpdate.getInt(OColumn.ROW_ID)) == 0)
                    continue;
                OArguments args = new OArguments();
                args.add(new JSONArray().put(selectServerId(qUpdate.getInt(OColumn.ROW_ID))));
                args.add(new JSONObject());
                if (dialog != null) {
                    if (countOrders < dialog.getMax())
                        ++countOrders;
                    dialog.setProgress(countOrders);
                }
                try {
                    if (dialog != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.setMessage("Confirm: " + qUpdate.getString("name"));
                            }
                        });
                    }
                    confirm = getServerDataHelper().callMethod("action_confirm", args);
                    if (dialog != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.setMessage("Create delivery: " + qUpdate.getString("name"));
                            }
                        });
                    }
                    createDelivery = getServerDataHelper().callMethod("create_delivery", args);
                    if (dialog != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.setMessage("Create invoice: " + qUpdate.getString("name"));
                            }
                        });
                    }
                    createInvoice = getServerDataHelper().callMethod("create_invoice", args);

                } catch (Exception e) {
                    e.printStackTrace();
                    SaleOrder.setSyncToServer(false);
                    onSyncFailed();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getContext(), R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
//                                    .show();
//                        }
//                    });
                    return;
                }

                if (confirm != null && confirm.equals(true)) {
                    OValues values = new OValues();
                    values.put("state", "sale");
                    values.put("state_title", getStateTitle(values));
                    if (createDelivery.equals(true) && createInvoice.equals(true)) {
                        values.put("invoice_status", "invoiced");
                        values.put("invoice_status_title", getInvoiceStatusTitle(values));
                        values.put("_is_local_only", "oldsync");
                    }
                    values.put("_is_dirty", "false");
                    update(qUpdate.getInt(OColumn.ROW_ID), values);
                    this.mType = Type.SyncSuccess;
                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getContext(), R.string.toast_problem_on_server_odoo, Toast.LENGTH_LONG)
//                                    .show();
//                        }
//                    });

                }
            }
        }
        SaleOrder.setSyncToServer(false);
        return;
    }

    public List<ODataRow> checkNewQuotations(Context context) {
        boolean CheckOk = false;
        try {
            SaleOrder sale = new SaleOrder(context, null);
            String sql = "SELECT name, id, _id, state, partner_id, date_order  FROM sale_order WHERE (state = ? or state = ? ) and _is_active = ?";
            have_id_zero_records = sale.query(sql, new String[]{"draft", "manual", "true"}); // crooked nail
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

    @Override
    public void onConnect(com.odoo.core.rpc.Odoo odoo) {
        Log.d(TAG, "exist_db returned TRUE ");
        mContext.startService(new Intent(mContext, SaleOrderSyncIntentService.class)
                .putExtra("syncType", SaleOrderSyncIntentService.SYNC_ONLY));
    }

    @Override
    public void onError(OdooError error) {
        Log.d(TAG, "exist_db returned FALSE ");
        Toast.makeText(getContext(), _s(R.string.label_quotation_fault), Toast.LENGTH_LONG).show();
    }

    public void deletePeriodOrders() {
        OPreferenceManager preferenceManager = new OPreferenceManager(mContext);
        SaleOrder order = new SaleOrder(getContext(), null);
        SQLiteDatabase db = getWritableDatabase();
        int data_limit = preferenceManager.getInt("sync_data_limit", 2);
        List<Integer> serverIds = new ArrayList<>();
        List<Integer> orderLineIds = new ArrayList<>();

        try {
            String sql = "SELECT id, _id FROM sale_order WHERE create_date >= '"
                    + ODateUtils.getDateBefore(data_limit) + "'";
            List<ODataRow> orderIds = order.query(sql);
            for (ODataRow row : orderIds) {
                serverIds.add(row.getInt("id"));
                orderLineIds.add(row.getInt(OColumn.ROW_ID));
            }
            String deleteOrderLineSql = "DELETE FROM sale_order_line" + " WHERE order_id " + "NOT IN (" +
                    TextUtils.join(",", orderLineIds) + ")";
            db.execSQL(deleteOrderLineSql);
            String deleteSql = "DELETE FROM sale_order" + " WHERE id " + "NOT IN (" +
                    TextUtils.join(",", serverIds) + ")";
            db.execSQL(deleteSql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnOperationSuccessListener {

        void OnSuccess();

        void OnFault();

        void OnCancelled();
    }

}

