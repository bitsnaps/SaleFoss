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
 * Created on 13/1/15 5:09 PM
 */
package com.odoo.addons.sale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.R;
import com.odoo.addons.sale.models.AccountPaymentTerm;
import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.addons.sale.models.ProductTemplate;
import com.odoo.addons.sale.models.SaleOrder;
import com.odoo.addons.sale.models.SalesOrderLine;
import com.odoo.addons.sale.models.StockMove;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.ServerDataHelper;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.ORecordValues;
import com.odoo.core.support.OUser;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.utils.JSONUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OAppBarUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.controls.ExpandableListControl;
import odoo.controls.OField;
import odoo.controls.OForm;

import static com.odoo.addons.sale.Sales.Type;

public class SalesDetail extends OdooCompatActivity implements View.OnClickListener {
    public static final String TAG = SalesDetail.class.getSimpleName();
    public static final int REQUEST_ADD_ITEMS = 323;
    private Bundle extra;
    private OForm mForm;
    private ODataRow record;
    private SaleOrder sale;
    private ActionBar actionBar;
    private ExpandableListControl mList;
    private ExpandableListControl.ExpandableListAdapter mAdapter;
    private List<Object> objects = new ArrayList<>();
    private HashMap<String, Float> lineValues = new HashMap<>();
    private HashMap<String, Integer> lineIds = new HashMap<>();
    private TextView txvType, currency1, currency2, currency3, untaxedAmt, taxesAmt, total_amt;
    private ODataRow currencyObj;
    private ResPartner partner = null;
    private ProductProduct products = null;
    private String mSOType = "";
    private LinearLayout layoutAddItem = null;
    private Type mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // check null second time
        setContentView(R.layout.sale_detail);
        OAppBarUtils.setAppBar(this, true);
        actionBar = getSupportActionBar();
        sale = new SaleOrder(this, null);
        extra = getIntent().getExtras();
        mType = Type.valueOf(extra.getString("type"));
        currencyObj = sale.currency();
        partner = new ResPartner(this, null);
        products = new ProductProduct(this, null);
        // Init() works too bad!
        try {
            init();
        } catch (Exception e){
            Toast.makeText(this, "Whoops!!!", Toast.LENGTH_LONG).show();
        }
        initAdapter();  // Original
    }

    private void init() {
        mForm = (OForm) findViewById(R.id.saleForm);
        mForm.setEditable(true);
        txvType = (TextView) findViewById(R.id.txvType);
        currency1 = (TextView) findViewById(R.id.currency1);
        currency2 = (TextView) findViewById(R.id.currency2);
        currency3 = (TextView) findViewById(R.id.currency3);
        String currencySymbol = currencyObj.getString("symbol");
        untaxedAmt = (TextView) findViewById(R.id.untaxedTotal);
        taxesAmt = (TextView) findViewById(R.id.taxesTotal);
        total_amt = (TextView) findViewById(R.id.fTotal);
        untaxedAmt.setText("0.00");
        taxesAmt.setText("0.00");
        total_amt.setText("0.00");
        layoutAddItem = (LinearLayout) findViewById(R.id.layoutAddItem);
        layoutAddItem.setOnClickListener(this);
        if (extra == null || !extra.containsKey(OColumn.ROW_ID)) {
            mForm.initForm(null);
            actionBar.setTitle(R.string.label_new);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_action_navigation_close);
            txvType.setText(R.string.label_quotation);
        } else {
            record = sale.browse(extra.getInt(OColumn.ROW_ID));
            if (record == null) {
                finish();
            }
            if (!record.getString("partner_id").equals("false") && mType == Type.Quotation) {
                OnCustomerChangeUpdate onCustomerChangeUpdate = new OnCustomerChangeUpdate();
                onCustomerChangeUpdate.execute(record.getM2ORecord("partner_id").browse());
            }
            if (mType == Type.Quotation) {
                actionBar.setTitle(R.string.label_quotation);
                txvType.setText(R.string.label_quotation);
                if (record.getString("state").equals("cancel"))
                    layoutAddItem.setVisibility(View.GONE);
            } else {
                layoutAddItem.setVisibility(View.GONE);
                actionBar.setTitle(R.string.label_sale_orders);
                txvType.setText(R.string.label_sale_orders);
                mForm.setEditable(false);
            }
            currencySymbol = record.getM2ORecord("currency_id").browse().getString("symbol");
            untaxedAmt.setText(String.format("%.2f", record.getFloat("amount_untaxed")));
            taxesAmt.setText(String.format("%.2f", record.getFloat("amount_tax")));
            total_amt.setText(String.format("%.2f", record.getFloat("amount_total")));

            mForm.initForm(record); //  Original - Call inin for Form - here is erro and axception for first launch

        }
        mSOType = txvType.getText().toString();
        currency1.setText(currencySymbol);
        currency2.setText(currencySymbol);
        currency3.setText(currencySymbol);
    }

    // here are writing lines of oder_line !!!
    private void initAdapter() {
        mList = (ExpandableListControl) findViewById(R.id.expListOrderLine);
        mList.setVisibility(View.VISIBLE);
        if (extra != null && record != null) {
            List<ODataRow> lines = record.getO2MRecord("order_line").browseEach();
            for (ODataRow line : lines) {
                int product_id = products.selectServerId(line.getInt("product_id"));
                if (product_id != 0) {
                    lineValues.put(product_id + "", line.getFloat("product_uom_qty"));
                    lineIds.put(product_id + "", line.getInt("id"));
                }
            }
            objects.addAll(lines);
        }
        mAdapter = mList.getAdapter(R.layout.sale_order_line_item, objects,
                new ExpandableListControl.ExpandableListAdapterGetViewListener() {
                    @Override
                    public View getView(int position, View mView, ViewGroup parent) {
                        ODataRow row = (ODataRow) mAdapter.getItem(position);
                        OControls.setText(mView, R.id.edtName, row.getString("name"));
                        OControls.setText(mView, R.id.edtProductQty, row.getString("product_uom_qty"));
                        OControls.setText(mView, R.id.edtProductPrice, String.format("%.2f", row.getFloat("price_unit")));
                        OControls.setText(mView, R.id.edtSubTotal, String.format("%.2f", row.getFloat("price_subtotal")));
                        return mView;
                    }
                });
        mAdapter.notifyDataSetChanged(objects);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sale_detail, menu);
        OField name = (OField) mForm.findViewById(R.id.fname);
        name.setEditable(false);
        if (extra != null && !extra.getString("type").equals(Type.SaleOrder.toString())) {
            // Operation on Sale Order
        } else {
            menu.findItem(R.id.menu_sale_save).setVisible(false);
            menu.findItem(R.id.menu_sale_confirm_sale).setVisible(false);
        }
        if (extra != null && record != null && record.getString("state").equals("cancel")) {
            menu.findItem(R.id.menu_sale_save).setVisible(true).setTitle("Copy Quotation");
            menu.findItem(R.id.menu_sale_detail_more).setVisible(false);
            mForm.setEditable(true);
        } else {
            menu.findItem(R.id.menu_sale_detail_more).setVisible(false);
            menu.findItem(R.id.menu_sale_new_copy_of_quotation).setVisible(false);
        }
        if (extra == null || !extra.containsKey(OColumn.ROW_ID)) {
            menu.findItem(R.id.menu_sale_save).setVisible(true);
            menu.findItem(R.id.menu_sale_detail_more).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        OValues values = mForm.getValues();
        App app = (App) getApplicationContext();
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_sale_save: // Save record Sale.Oder
                if (values != null) {
                    if (app.inNetwork() || !app.inNetwork()) {
                        values.put("partner_name", partner.getName(values.getInt("partner_id")));

                        SaleOrderOperation saleOrderOperation = new SaleOrderOperation();
                        saleOrderOperation.execute(values);

                    } else {
                        Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.menu_sale_confirm_sale:
                if (record != null) {
                    if (extra != null && record.getFloat("amount_total") > 0) {
                        if (app.inNetwork()) {
                            sale.confirmSale(record, confirmSale);
                        } else {
                            Toast.makeText(this, R.string.toast_network_required, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        OAlert.showWarning(this, R.string.label_no_order_line + "");
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class SaleOrderOperation extends AsyncTask<OValues, Void, Boolean> {

        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(SalesDetail.this);
            mDialog.setTitle(R.string.title_working);
            mDialog.setMessage("Creating lines");
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(OValues... params) {
            try {
                Thread.sleep(500);
                OValues values = params[0];
                // Creating oneToMany order lines
                JSONArray order_line = new JSONArray();
                for (Object line : objects) {
                    JSONArray o_line = new JSONArray();
                    ODataRow row = (ODataRow) line;
                    String product_id = row.getString("product_id");
                    o_line.put((lineIds.containsKey(product_id)) ? 1 : 0);
                    o_line.put((lineIds.containsKey(product_id)) ? lineIds.get(product_id) : false);
                    if (lineIds.containsKey(product_id)) {
                        JSONObject line_data = new JSONObject();
                        line_data.put("product_uom_qty", row.get("product_uom_qty"));
                        line_data.put("product_uos_qty", row.get("product_uos_qty"));
                        o_line.put(line_data);
                    } else
                        o_line.put(JSONUtils.toJSONObject(row));
                    order_line.put(o_line);
                    lineIds.remove(product_id);
                }
                if (lineIds.size() > 0) {
                    for (String key : lineIds.keySet()) {
                        JSONArray o_line = new JSONArray();
                        o_line.put(2);
                        o_line.put(lineIds.get(key));
                        o_line.put(false);
                        order_line.put(o_line);
                    }
                }
                Thread.sleep(500);
                ORecordValues data = new ORecordValues();

                if (values.getString("name").equals("/")) { // it means New record will create !!!
                    String nameOrder = sale.newNameSaleOrder("OFFLINE/SO");
                    values.put("name", nameOrder);
                    data.put("name", nameOrder);
                }
                else {
                    data.put("name", values.getString("name"));
                }

                data.put("partner_id", partner.selectServerId(values.getInt("partner_id")));
                data.put("date_order", values.getString("date_order"));
                data.put("payment_term", values.get("payment_term"));
                data.put("order_line", order_line);

                if (record == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setMessage("Creating " + mSOType);
                        }
                    });
                    Thread.sleep(500); ///
                    //int new_id = sale. insert(values);
                    int new_id = sale.getServerDataHelper().createOnServer(data); /// want to go on server!! we need
                    values.put("id", new_id);
                    ODataRow record = new ODataRow();
                    record.put("id", new_id);
                    sale.quickCreateRecord(record); /// want to go on server!! we need
                    //sale.insert(values); //

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setMessage("Updating " + mSOType);
                        }
                    });
                    Thread.sleep(500);
                    //sale.insert(values);
                    sale.getServerDataHelper().updateOnServer(data, record.getInt("id"));
                    sale.quickCreateRecord(record);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mDialog.dismiss();
            if (success) {
                Toast.makeText(SalesDetail.this, (record != null) ? mSOType + " updated"
                        : mSOType + " created", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    SaleOrder.OnOperationSuccessListener cancelOrder = new SaleOrder.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            Toast.makeText(SalesDetail.this, StringUtils.capitalizeString(extra.getString("type"))
                    + " cancelled", Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void OnCancelled() {

        }
    };

    SaleOrder.OnOperationSuccessListener confirmSale = new SaleOrder.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            Toast.makeText(SalesDetail.this, R.string.label_quotation_confirm, Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void OnCancelled() {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutAddItem:
                if (mForm.getValues() != null) {
                    Intent intent = new Intent(this, AddProductLineWizard.class);
                    Bundle extra = new Bundle();
                    for (String key : lineValues.keySet()) {
                        extra.putFloat(key, lineValues.get(key));
                    }
                    intent.putExtras(extra);
                    startActivityForResult(intent, REQUEST_ADD_ITEMS);
                }
                break;
        }
    }

    private class OnCustomerChangeUpdate extends AsyncTask<ODataRow, Void, Void> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SalesDetail.this);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_working));
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(ODataRow... params) {
            sale.onPartnerIdChange(params[0]);  // Original
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)  {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
        }
    }

    private class OnProductChange extends AsyncTask<HashMap<String, Float>, Void, List<ODataRow>> {
        private ProgressDialog progressDialog;
        private String warning = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SalesDetail.this);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(R.string.title_please_wait);
            progressDialog.setMessage(OResource.string(SalesDetail.this, R.string.title_working));
            progressDialog.show();
        }

        @Override
        protected List<ODataRow> doInBackground(HashMap<String, Float>... params) {
            final OValues[] formValues = new OValues[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    formValues[0] = mForm.getValues();
                }
            });

            List<ODataRow> items = new ArrayList<>();
            try {
                Thread.sleep(1000);
                ProductProduct productProduct = new ProductProduct(SalesDetail.this, sale.getUser());
                //SalesOrderLine saleLine = new SalesOrderLine(SalesDetail.this, sale.getUser());
                ResPartner partner = new ResPartner(SalesDetail.this, sale.getUser());
               //ProductTemplate prodTempl = new ProductTemplate(SalesDetail.this, sale.getUser());
               // StockMove stockMove = new StockMove(SalesDetail.this, sale.getUser());

                //ODataRow customer = partner.browse(formValues[0].getInt("partner_id"));

                for (String key : params[0].keySet()) {
                    ODataRow product = productProduct.browse(productProduct.selectRowId(Integer.parseInt(key)));
                    Float qty = params[0].get(key);
                    //int pricelist = customer.getInt("pricelist_id");
                    HashMap<String, Object> context = new HashMap<>();
                    //context.put("partner_id", customer.getInt("id"));
                    context.put("quantity", qty);
                    //context.put("pricelist", pricelist);

                    // Fill in row values for insert in the local DB
                    OValues values = new OValues();
                    values.put("product_id", product.getInt("id"));
                    values.put("name", product.get("name_template")); // it is // mine

                    //OControls.setText(mView, R.id.edtProductQty, row.getString("product_uom_qty"));
                    //OControls.setText(mView, R.id.edtProductPrice, String.format("%.2f", row.getFloat("price_unit")));
                    //OControls.setText(mView, R.id.edtSubTotal, String.format("%.2f", row.getFloat("price_subtotal")));

                    values.put("product_uom_qty", qty);
                    values.put("product_uom", false);
                    values.put("price_unit", product.getFloat("lst_price"));
                    values.put("product_uos_qty", qty);
                    values.put("product_uos", false);
                    values.put("price_subtotal", product.getFloat("lst_price") * qty); //res.getDouble("product_uos_qty");

                    JSONArray tax_id = new JSONArray();
                    tax_id.put(6);
                    tax_id.put(false);
                    tax_id.put(false);
                    values.put("tax_id", new JSONArray().put(tax_id));
                    values.put("th_weight", 0);
                    values.put("discount", 0);

                    if (extra != null)
                        values.put("order_id", extra.getInt(OColumn.ROW_ID));
                    items.add(values.toDataRow());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(List<ODataRow> row) {
         try{
            super.onPostExecute(row);
            if (row != null) {
                objects.clear();
                objects.addAll(row);
                mAdapter.notifyDataSetChanged(objects);  //original
                float total = 0.0f;
                for (ODataRow rec : row) {
                    total += rec.getFloat("price_subtotal");
                }
                total_amt.setText(String.format("%.2f", total));
                untaxedAmt.setText(total_amt.getText());
            }
            progressDialog.dismiss();
            if (warning != null) {
                OAlert.showWarning(SalesDetail.this, warning.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
            progressDialog.dismiss();
        }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_ITEMS && resultCode == Activity.RESULT_OK) {
            lineValues.clear();
            for (String key : data.getExtras().keySet()) {
                if (data.getExtras().getFloat(key) > 0)
                    lineValues.put(key, data.getExtras().getFloat(key));
            }
            OnProductChange onProductChange = new OnProductChange();
            onProductChange.execute(lineValues);
        }
    }

}
