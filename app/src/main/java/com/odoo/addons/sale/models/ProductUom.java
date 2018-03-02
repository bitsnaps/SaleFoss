package com.odoo.addons.sale.models;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by lavrentievd on 01.03.2018.
 */

public class ProductUom  extends OModel {
    public static final String TAG = SalesOrderLine.class.getSimpleName();

    OColumn name = new OColumn("Description ", OVarchar.class).setSize(128);

    public ProductUom(Context context, OUser user) {
        super(context, "product.uom", user);
    }
}
