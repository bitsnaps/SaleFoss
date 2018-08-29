package com.odoo.addons.sale.providers;

import android.net.Uri;

import com.odoo.addons.sale.models.ProductProduct;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.provider.BaseModelProvider;

public class ProductProvider  extends BaseModelProvider {
    public static final String TAG = ProductProvider.class.getSimpleName();

    @Override
    public OModel getModel(Uri uri) {
        return new ProductProduct(getContext(), getUser(uri));
    }

    @Override
    public String authority() {
        return ProductProduct.AUTHORITY;
    }

}
