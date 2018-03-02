package com.odoo.addons.sale.models;

/**
 * Created by lavrentievd on 01.03.2018.
 */

import android.content.Context;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;


public class StockMove extends OModel {
    public static final String TAG = SalesOrderLine.class.getSimpleName();

    OColumn name = new OColumn("Description ", OVarchar.class).setSize(128);
    OColumn product_id = new OColumn("Product", ProductProduct.class,
            OColumn.RelationType.ManyToOne);
    OColumn product_qty = new OColumn("Quantity", OFloat.class);
    OColumn product_uom = new OColumn("Unit of Measure", ProductUom.class,
            OColumn.RelationType.ManyToOne);

    OColumn product_uos_qty = new OColumn("Quantity (UOS)", OFloat.class);
    OColumn price_unit = new OColumn("Unit Price", OFloat.class);

    public StockMove(Context context, OUser user) {
        super(context, "stock.move", user);
    }

    @Override
    public ODomain defaultDomain(){
        ODomain domain = new ODomain();
        return domain;
    }

}
