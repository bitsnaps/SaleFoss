/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 13/1/15 11:11 AM
 */
package com.odoo.addons.sale.models;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;

import java.util.ArrayList;

public class ProductProduct extends OModel {
    public static final String TAG = ProductProduct.class.getSimpleName();

    OColumn product_tmpl_id = new OColumn("Product Template",  ProductTemplate.class,
            OColumn.RelationType.ManyToOne);
    @Odoo.Functional(method = "storeProductName", store = true, depends = {"product_tmpl_id"})
    OColumn name_template = new OColumn("Name", OVarchar.class).setSize(128).setLocalColumn();

    OColumn default_code = new OColumn("Internal Reference", OVarchar.class).setSize(64);
    OColumn lst_price = new OColumn("Public price", OFloat.class);
    OColumn sale_ok = new OColumn("Stock OK", OBoolean.class).setDefaultValue(false);



    public ProductProduct(Context context, OUser user) {
        super(context, "product.product", user);
        setDefaultNameColumn("name_template");
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
    public ODomain defaultDomain(){
        ODomain domain = new ODomain();
        return domain;
    }

}
