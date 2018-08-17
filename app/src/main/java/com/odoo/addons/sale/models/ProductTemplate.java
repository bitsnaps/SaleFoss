package com.odoo.addons.sale.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;

/**
 * Created by lavrentievd on 01.02.2018.
 */

public class ProductTemplate  extends OModel {

    public static final String TAG = ProductTemplate.class.getSimpleName();

    OColumn name = new OColumn("Name", OVarchar.class).setSize(128);
    OColumn list_price = new OColumn("Sale Price", OFloat.class);
    OColumn standard_price = new OColumn("Cost", OFloat.class);
    OColumn sale_ok = new OColumn("Can be Sold", OBoolean.class);
    OColumn active = new OColumn("Active", OBoolean.class);

    @Override
    public ODomain defaultDomain(){
        ODomain domain = new ODomain();
        return domain;
    }

    public ProductTemplate(Context context, OUser user) {
        super(context, "product.template", user);
    }
}
