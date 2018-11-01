package com.odoo.addons.sale;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.core.utils.OAppBarUtils;

public class ResultSync extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sale_sync_result);
        OAppBarUtils.setAppBar(this, true);
        setTitle(R.string.sync_data_title);
        TextView messageSuccess;
        messageSuccess = (TextView) findViewById(R.id.textView2);
        messageSuccess.setText(R.string.notif_sync_success);
        messageSuccess.setTextColor(getResources().getColor(R.color.android_green_dark));
        messageSuccess.setTextSize(24);
    }
}
