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
 * Created on 13/1/15 11:08 AM
 */
package com.odoo.addons.sale.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;

public class SalesOrderLine extends OModel {
    public static final String TAG = SalesOrderLine.class.getSimpleName();
    private Context idContext = getContext();

    OColumn product_id = new OColumn(_s(R.string.field_label_product_id), ProductProduct.class,
            OColumn.RelationType.ManyToOne);
    OColumn name = new OColumn(_s(R.string.field_label_name), OText.class);
    OColumn product_uom_qty = new OColumn(_s(R.string.field_label_product_uom_qty), OInteger.class);
    OColumn price_unit = new OColumn(_s(R.string.field_label_price_unit), OFloat.class);
    OColumn price_subtotal = new OColumn(_s(R.string.field_label_price_subtotal), OFloat.class);
    OColumn order_id = new OColumn(_s(R.string.field_label_order_id), SaleOrder.class,
            OColumn.RelationType.ManyToOne);

    private String _s(int res_id) {
        return OResource.string(idContext, res_id);
    }

    @Override
    public ODomain defaultDomain(){
        ODomain domain = new ODomain();
        return domain;
    }

    public SalesOrderLine(Context context, OUser user) {
        super(context, "sale.order.line", user);
    }
}
