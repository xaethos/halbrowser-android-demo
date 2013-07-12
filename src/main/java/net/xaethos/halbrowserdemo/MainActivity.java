package net.xaethos.halbrowserdemo;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class MainActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] apis = getResources().getStringArray(R.array.apis);
        final ListAdapter adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, apis);

        setListAdapter(adapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Intent intent = new Intent(MainActivity.this, BrowserActivity.class);
                intent.putExtra(BrowserActivity.EXTRA_API_URL, apis[pos]);
                startActivity(intent);
            }
        });
    }
}