package com.odoo.addons.sale.models;

import android.content.Context;

import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;

import android.content.Context;

import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.support.OUser;

/**
 * Created by lavrentievd on 01.02.2018.
 */

class FirstLoading extends OModel {

    public static final String TAG = FirstLoading.class.getSimpleName();

    OColumn name = new OColumn("Name", OVarchar.class).setSize(128);
    OColumn is_fist = new OColumn("Fist Loading", OBoolean.class);

    public FirstLoading(Context context, String model_name, OUser user) {
        super(context, "product.template", user);
    }
}
