package com.odoo.addons.sale.services;

import android.content.Context;
import android.os.Bundle;

import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

public class ProductSyncService extends OSyncService {

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, ProductProduct.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        ODomain domain = new ODomain();
        adapter.setDomain(domain).syncDataLimit(750);
    }
}

