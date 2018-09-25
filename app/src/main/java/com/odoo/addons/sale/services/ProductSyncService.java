package com.odoo.addons.sale.services;

import android.content.Context;
import android.os.Bundle;

import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.addons.sale.models.SaleOrder;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

import java.util.ArrayList;
import java.util.List;

public class ProductSyncService extends OSyncService {

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, ProductProduct.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        ODomain domain = new ODomain();
        ProductProduct product = new ProductProduct(getApplicationContext(), user); // Original

        List<Integer> newIds = new ArrayList<>();
        for (ODataRow row : product.select(new String[]{})) {
            newIds.add(row.getInt("id"));
        }
        if (newIds.size() > 0) {
            domain.add("id", "not in", newIds);
        }
        adapter.setDomain(domain).syncDataLimit(3000);
    }
}

