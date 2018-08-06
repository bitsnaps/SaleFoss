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
 * Created on 2/1/15 11:07 AM
 */
package com.odoo.addons.customers.services;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

public class CustomerSyncService extends OSyncService {
    public static final String TAG = CustomerSyncService.class.getSimpleName();

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, ResPartner.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        if (adapter.getModel().getModelName().equals("res.partner")) {
            ODomain domain = new ODomain();
            ResPartner resPartner = new ResPartner(getApplicationContext(), user);
            domain.add("customer", "not in", ("false"));
            resPartner.quickSyncRecords(domain);

//            domain.add("|");
//            domain.add("|");
//            domain.add("opportunity_ids.user_id", "=", user.getUserId());
//            domain.add("sale_order_ids.user_id", "=", user.getUserId());
//            domain.add("id", "in", adapter.getModel().getServerIds());
//            adapter.setDomain(domain).syncDataLimit(200);
        }
    }
}
