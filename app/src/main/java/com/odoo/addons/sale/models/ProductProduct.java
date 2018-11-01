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

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
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

    public void syncProduct(boolean isToasts) {
        int items;
        ODomain domain = new ODomain();
        OArguments args = new OArguments();
        args.add(new JSONObject());
        if (isToasts) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), _s(R.string.label_product_download_start), Toast.LENGTH_LONG).show();
                }
            });
        }
        try {
            ProductTemplate productTemplate = new ProductTemplate(getContext(), getUser());
            OdooFields fields = new OdooFields(new String[]{"id"});
            List<Object> maxDate = new ArrayList<>();

            List<ODataRow> dates = productTemplate.getServerDataHelper().searchRecords(fields, domain, 3000);
            if (productTemplate.getServerIds().size() == dates.size()) {
                String sql = "SELECT max(write_date) as maxDate FROM product_template";
                List<ODataRow> records = productTemplate.query(sql);

                for (ODataRow row : records) {
                    maxDate.add(row.get("maxDate"));
                }
                ODomain domainDate = new ODomain();
                domainDate.add("write_date", ">", maxDate.get(0));
                List<Integer> newIds = new ArrayList<>();
                fields = new OdooFields(new String[]{"id, write_date"});
                dates = productTemplate.getServerDataHelper().searchRecords(fields, domainDate, 150);
                for (ODataRow row : dates) {
                    newIds.add(((Double) row.get("id")).intValue());
                }
                items = newIds.size();
                if (items > 0) {
                    domain.add("product_tmpl_id", "in", newIds);
                }
            } else {
                domain.add("product_tmpl_id", "not in", productTemplate.getServerIds());
            }
            quickSyncRecords(domain);
            if (isToasts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), _s(R.string.label_product_download_end), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isToasts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), _s(R.string.label_product_download_fault), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

}


