package com.odoo.addons.sale;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.odoo.R;
import com.odoo.core.utils.OAppBarUtils;

public class ResultSync extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sale_sync_result);
        OAppBarUtils.setAppBar(this, true);
        setTitle(R.string.sync_data_title);
    }
}
