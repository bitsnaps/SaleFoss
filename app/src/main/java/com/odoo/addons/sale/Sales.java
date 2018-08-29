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
package com.odoo.addons.sale;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.addons.sale.models.SaleOrder;
import com.odoo.addons.sale.models.SalesOrderLine;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.IOnItemClickListener;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.IntentUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.controls.OBottomSheet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sales extends BaseFragment implements
        OCursorListAdapter.OnViewBindListener, LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener, IOnSearchViewChangeListener,
        ISyncStatusObserverListener, IOnItemClickListener, View.OnClickListener,
        OBottomSheet.OSheetActionClickListener, OBottomSheet.OSheetItemClickListener {
    public static final String TAG = Sales.class.getSimpleName();
    public static final String KEY_MENU = "key_sales_menu";
    public Bundle dataGlob;
    public List<ODataRow> have_id_zero_records = null;
    SaleOrder.OnOperationSuccessListener confirmSale = new SaleOrder.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            Toast.makeText(getActivity(), _s(R.string.label_quotation_confirmed), Toast.LENGTH_LONG).show();
        }

        @Override
        public void OnCancelled() {
        }
    };
    SaleOrder.OnOperationSuccessListener newCopyQuotation = new SaleOrder.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            Toast.makeText(getActivity(), R.string.label_copy_quotation, Toast.LENGTH_LONG).show();
        }

        @Override
        public void OnCancelled() {
        }
    };
    private View mView;
    private ListView mList;
    private OCursorListAdapter mAdapter;
    private String mFilter = null;
    private Type mType = Type.Quotation;
    SaleOrder.OnOperationSuccessListener cancelOrder = new SaleOrder.OnOperationSuccessListener() {
        @Override
        public void OnSuccess() {
            Toast.makeText(getActivity(), mType + " " + _s(R.string.label_canceled), Toast.LENGTH_LONG).show();
        }

        @Override
        public void OnCancelled() {
        }
    };
    private SaleOrder sale = null;
    private Boolean mSyncRequested = false;
    private int have_zero = 0;
    private boolean haveNewQuotations = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mType = Type.valueOf(getArguments().getString(KEY_MENU));
        return inflater.inflate(R.layout.common_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        initAdapter();
    }

    private void initAdapter() {
        mList = (ListView) mView.findViewById(R.id.listview);
        mAdapter = new OCursorListAdapter(getActivity(), null, R.layout.sale_order_item);
        mAdapter.setOnViewBindListener(this);
        mList.setAdapter(mAdapter);
        mAdapter.handleItemClickListener(mList, this);

        setHasFloatingButton(mView, R.id.syncButton, mList, this);
        if (mType == Type.Quotation)
            mView.findViewById(R.id.syncButton).setVisibility(View.GONE);

        setHasFloatingButton(mView, R.id.fabButton, mList, this);
        if (mType == Type.SaleOrder)
            mView.findViewById(R.id.fabButton).setVisibility(View.GONE);

        setHasSyncStatusObserver(TAG, this, db());
        setHasSwipeRefreshView(mView, R.id.swipe_container, this);
        getLoaderManager().initLoader(0, null, this);

//        if (inNetwork() && checkNewQuotations(getContext()) != null) {
//            if (mType == Type.Quotation)
//                mView.findViewById(R.id.syncButton).setVisibility(View.VISIBLE);
//
//        } else {
//            if (mType == Type.Quotation)
//                mView.findViewById(R.id.syncButton).setVisibility(View.GONE);
//
//        }

        mView.findViewById(R.id.syncButton).setVisibility(View.GONE);

    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        SaleOrder saleOrder = new SaleOrder(getContext(), null);
        OValues state = new OValues();
        state.put("state", row.getString("state"));
        state.put("invoice_status", row.getString("invoice_status"));
        state.put("order_line", row.getString("order_line"));

        OControls.setText(view, R.id.name, row.getString("name"));
        String format = (db().getUser().getOdooVersion().getVersionNumber() <= 7)
                ? ODateUtils.DEFAULT_DATE_FORMAT : ODateUtils.DEFAULT_FORMAT;
        String date = ODateUtils.convertToDefault(row.getString("date_order"),
                format, "MMMM, dd, HH:mm");

        OControls.setText(view, R.id.date_order, date);

        if (row.getString("state").equals("sale")) {
            OControls.setText(view, R.id.state, saleOrder.getInvoiceStatusTitle(state));
        } else {
            OControls.setText(view, R.id.state, saleOrder.getStateTitle(state));
        }
//        OControls.setText(view, R.id.state, saleOrder.getStateTitle(state));

        if (row.getString("partner_name").equals("false")) {
            OControls.setGone(view, (R.id.partner_name));
        } else {
            OControls.setVisible(view, R.id.partner_name);
            OControls.setText(view, R.id.partner_name, row.getString("partner_name"));
        }
        OControls.setText(view, R.id.amount_total, row.getString("amount_total"));
        if (row.getString("currency_symbol").equals("false")) {
            OControls.setGone(view, (R.id.currency_symbol));
        } else {
            OControls.setVisible(view, R.id.currency_symbol);
            OControls.setText(view, R.id.currency_symbol, row.getString("currency_symbol"));
        }
        OControls.setText(view, R.id.order_lines, row.getString("order_line_count"));
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> menu = new ArrayList<>();
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_quotation))
                .setIcon(R.drawable.ic_action_quotation)
                .setInstance(new Sales())
                .setExtra(data(Type.Quotation)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_sale_orders))
                .setIcon(R.drawable.ic_action_sale_order)
                .setInstance(new Sales())
                .setExtra(data(Type.SaleOrder)));

        return menu;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = null;
        String[] whereArgs = null;
        List<String> args = new ArrayList<>();
        switch (mType) {
            case Quotation:
                where = " (state = ? or state = ? or state = ?)";
                args.addAll(Arrays.asList(new String[]{"draft", "sent", "cancel"}));
                break;
            case SaleOrder:
                where = "(state = ? or state = ? or state = ? or state = ?)";
                args.addAll(Arrays.asList(new String[]{"manual", "sale", "progress",
                        "done"}));
                break;
        }
        if (mFilter != null) {
            where += " and (name like ? or partner_name like ? or state_title like ?)";
            args.add("%" + mFilter + "%");
            args.add("%" + mFilter + "%");
            args.add("%" + mFilter + "%");
        }
        whereArgs = args.toArray(new String[args.size()]);
        return new CursorLoader(getActivity(), db().uri(), null, where, whereArgs, "date_order DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        if (data.getCount() > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(mView, R.id.swipe_container);
                    OControls.setGone(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Sales.this);
                }
            }, 500);

        } else {
            if (db().isEmptyTable() && !mSyncRequested) {
                mSyncRequested = true;
                if (sale == null)
                    sale = new SaleOrder(getContext(), null);
//                parent().sync().requestSync(SaleOrder.AUTHORITY); // Check for need
                onRefresh();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.data_list_no_item);
                    setHasSwipeRefreshView(mView, R.id.data_list_no_item, Sales.this);
                    OControls.setImage(mView, R.id.icon,
                            (mType == Type.Quotation) ? R.drawable.ic_action_quotation : R.drawable.ic_action_sale_order);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no) + " " + _s(R.string.label_found));
//                    OControls.setText(mView, R.id.title, _s(R.string.label_no) + mType + getString(R.string.label_found));
                    OControls.setText(mView, R.id.subTitle, "");
                }
            }, 500);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private Bundle data(Type type) {
        Bundle extra = new Bundle();
        extra.putString(KEY_MENU, type.toString());
        return extra;
    }

    @Override
    public Class<SaleOrder> database() {
        return SaleOrder.class;
    }

    @Override
    public void onRefresh() {
        List<ODataRow> CheckNewRecords = null;
        Context context = getContext();
        if (inNetwork()) {
            try {
                Thread.sleep(600);
                setSwipeRefreshing(false); //true need
//                syncProductNew(context);
//                sale.syncOrders(context);

                parent().sync().requestSync(ProductProduct.AUTHORITY); // Check for need
                parent().sync().requestSync(SaleOrder.AUTHORITY); // Check for need
                CheckNewRecords = checkNewQuotations(context);
                if (CheckNewRecords != null) {
//                    if (mType == Type.Quotation)
//                        mView.findViewById(R.id.syncButton).setVisibility(View.VISIBLE);
//                    Toast.makeText(getActivity(), _s(R.string.toast_update_database), Toast.LENGTH_LONG)
//                            .show();
                } else {
                    if (mType == Type.Quotation)
                        mView.findViewById(R.id.syncButton).setVisibility(View.GONE);
//                    Toast.makeText(getActivity(), _s(R.string.toast_no_new_records), Toast.LENGTH_LONG)
//                            .show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), _s(R.string.label_crash_refresh), Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            hideRefreshingProgress();
            if (mType == Type.Quotation)
                mView.findViewById(R.id.syncButton).setVisibility(View.GONE);
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_sales_order, menu);
        setHasSearchView(this, menu, R.id.menu_sales_search);

    }

    @Override
    public boolean onSearchViewTextChange(String newFilter) {
        mFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onSearchViewClose() {

        //Nothing to do
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemDoubleClick(View view, int position) {
        onDoubleClick(position);
    }

    private void onDoubleClick(int position) {
        ODataRow row = OCursorUtils.toDatarow((Cursor) mAdapter.getItem(position));
        Bundle data = row.getPrimaryBundleData();
        data.putString("type", mType.toString());
        IntentUtils.startActivity(getActivity(), SalesDetail.class, data);
    }

    @Override
    public void onItemClick(View view, int position) {
        if (mType == Type.Quotation)
            onDoubleClick(position);
            //showSheet((Cursor) mAdapter.getItem(position));
        else
            onDoubleClick(position);
    }

    private void showSheet(Cursor data) {
        OBottomSheet bottomSheet = new OBottomSheet(getActivity());
        bottomSheet.setData(data);
        bottomSheet.setSheetTitle(data.getString(data.getColumnIndex("name")));
        bottomSheet.setActionIcon(R.drawable.ic_action_edit, this);
        bottomSheet.setSheetItemClickListener(this);

        if (data.getString(data.getColumnIndex("state")).equals("cancel"))
            bottomSheet.setSheetActionsMenu(R.menu.menu_quotation_cancel_sheet);
        else
            bottomSheet.setSheetActionsMenu(R.menu.menu_quotation_sheet);
        bottomSheet.show();
    }

    @Override
    public void onSheetItemClick(OBottomSheet sheet, MenuItem item, Object data) {
        sheet.dismiss();
        ODataRow row = OCursorUtils.toDatarow((Cursor) data);
        switch (item.getItemId()) {
            case R.id.menu_quotation_cancel:
                ((SaleOrder) db()).cancelOrder(mType, row, cancelOrder);
                break;
            case R.id.menu_quotation_new:
                if (inNetwork()) {
                    ((SaleOrder) db()).newCopyQuotation(row, newCopyQuotation);
                } else {
                    Toast.makeText(getActivity(), R.string.toast_network_required, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.menu_so_confirm_sale:
                if (row.getFloat("amount_total") > 0) {
                    if (inNetwork()) {
                        ((SaleOrder) db()).confirmSale(row, confirmSale);
                    } else {
                        Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG).show();
                    }
                } else {
                    OAlert.showWarning(getActivity(), getString(R.string.label_no_lines));
                }
                break;
        }
    }

    @Override
    public void onSheetActionClick(OBottomSheet sheet, Object data) {
        sheet.dismiss();
        if (data instanceof Cursor) {
            try {
                ODataRow row = OCursorUtils.toDatarow((Cursor) data);
                Bundle extras = row.getPrimaryBundleData();
                extras.putString("type", mType.toString());
                IntentUtils.startActivity(getActivity(), SalesDetail.class, extras);
            } catch (Exception e) {
                e.printStackTrace();
                sheet.dismiss();
            }
        }
    }

    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();

        switch (v.getId()) {
            case R.id.fabButton:
                bundle.putString("type", Type.Quotation.toString());
                IntentUtils.startActivity(getActivity(), SalesDetail.class, bundle);
                break;
            case R.id.syncButton:
                if (inNetwork() && checkNewQuotations(getContext()) != null) {
                    if (mType == Type.Quotation)
                        mView.findViewById(R.id.syncButton).setVisibility(View.VISIBLE);
                    bundle.putString("type", Type.Quotation.toString());
                    syncLocalDatatoOdoo(getContext(), have_id_zero_records);
                } else {
                    if (mType == Type.Quotation)
                        mView.findViewById(R.id.syncButton).setVisibility(View.GONE);
                    Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                            .show();
                }
                break;
        }
    }

    public List<ODataRow> getIdZeroRecords() {
        return null;
    }


    public List<ODataRow> checkNewQuotations(Context context) {
        boolean CheckOk = false;
        try {
            SaleOrder sale = new SaleOrder(context, null);
            String sql = "SELECT name, _id, state FROM sale_order WHERE id = ? or state = ?";
            have_id_zero_records = sale.query(sql, new String[]{"0", "draft"});
            have_zero = have_id_zero_records.size();
            if (have_zero != 0) {
                CheckOk = true;
            }
            if (mView != null) {
//                mView.findViewById(R.id.syncButton).setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (CheckOk)
            return have_id_zero_records;
        return null;
    }

    public void syncLocalDatatoOdoo(final Context context, final List<ODataRow> quotation) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(context);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(context, R.string.title_loading));
                dialog.setCancelable(false); // original false
                setSwipeRefreshing(true);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                final TextView mLoginProcessStatus = null;

                try {
                    Thread.sleep(500);
                    ODomain domain = new ODomain();
                    SalesOrderLine salesOrderLine = new SalesOrderLine(context, null); // getuser
                    SaleOrder saleOrder = new SaleOrder(context, null);
                    Object confirm = null;
//done recently
                    domain.add("id", "=", "0");

                    salesOrderLine.quickSyncRecords(domain);
                    saleOrder.quickSyncRecords(domain);

//                    doWorkflowFullConfirmEachOrder(saleOrder, context, quotation);
                    doWorkflowFullConfirm(saleOrder, context, quotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    hideRefreshingProgress();
                    haveNewQuotations = false;
                    if (mType == Type.Quotation)
                        if (mView != null)
                            mView.findViewById(R.id.syncButton).setVisibility(View.GONE);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Не переворачиваем!!!!!!!", Toast.LENGTH_LONG)
                            .show();
                }
                Toast.makeText(context, R.string.toast_recs_updated, Toast.LENGTH_LONG)
                        .show();

            }
        }.execute();
    }

    public void syncProduct(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(context);
                dialog.setTitle(R.string.title_please_wait);
                dialog.setMessage(OResource.string(context, R.string.title_loading_product));
                dialog.setCancelable(false); // original false
                setSwipeRefreshing(true);
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    Thread.sleep(300);
                    ODomain domain = new ODomain();
                    ProductProduct product = new ProductProduct(context, null);
                    product.quickSyncRecords(domain);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                hideRefreshingProgress();
                dialog.dismiss();
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

    private void doWorkflowFullConfirmEachOrder(SaleOrder model, Context context, final List<ODataRow> quotation) {

        if (checkNewQuotations(context) != null) {
            Object createInvoice;
            Object createDelivery;

            for (final ODataRow qUpdate : quotation) {
                JSONArray idList = new JSONArray();
                OArguments args = new OArguments();
                idList.put(model.selectServerId(qUpdate.getInt(OColumn.ROW_ID)));
                args.add(idList);
                args.add(new JSONObject());
                Object confirm = model.getServerDataHelper().callMethod("action_confirm", args);

                createDelivery = model.getServerDataHelper().callMethod("create_delivery", args);
                createInvoice = model.getServerDataHelper().callMethod("create_invoice", args);

//                Object confirmWorkFlow = model.getServerDataHelper().callMethod("create_with_full_confirm", args);

                if (confirm != null && confirm.equals(true)) {
                    OValues values = new OValues();
                    values.put("state", "sale");
                    values.put("state_title", model.getStateTitle(values));
                    values.put("_is_dirty", "false");
                    model.update(qUpdate.getInt(OColumn.ROW_ID), values);
                }
            }
        }
    }

    private void doWorkflowFullConfirm(SaleOrder model, Context context, final List<ODataRow> quotation) {
        Object confirm = null;
        Object createInvoice;
        Object createDelivery;
        Object comfirm_full;

        if (checkNewQuotations(context) != null) {
            JSONArray idList = new JSONArray();
            OArguments args = new OArguments();
            for (final ODataRow qUpdate : quotation) {
                idList.put(model.selectServerId(qUpdate.getInt(OColumn.ROW_ID)));
            }
            args.add(idList);
            args.add(new JSONObject());

            confirm = model.getServerDataHelper().callMethod("action_confirm", args);

            comfirm_full = model.getServerDataHelper().callMethod("create_with_full_confirm", args);

//            createDelivery = model.getServerDataHelper().callMethod("create_delivery", args);
//            createInvoice = model.getServerDataHelper().callMethod("create_invoice", args);

            if (confirm != null && confirm.equals(true)) {
                for (final ODataRow qUpdate : quotation) {
                    OValues values = new OValues();
                    values.put("state", "sale");
                    values.put("state_title", model.getStateTitle(values));
                    if (comfirm_full.equals(true)) {
                        values.put("invoice_status", "invoiced");
                        values.put("invoice_status_title", model.getInvoiceStatusTitle(values));
                    }
                    values.put("_is_dirty", "false");
                    model.update(qUpdate.getInt(OColumn.ROW_ID), values);
                }
            }
        }
    }


    public enum Type {
        Quotation,
        SaleOrder
    }

}



