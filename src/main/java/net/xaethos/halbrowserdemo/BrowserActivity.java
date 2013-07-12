package net.xaethos.halbrowserdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import net.xaethos.android.halbrowser.APIClient;
import net.xaethos.android.halbrowser.Relation;
import net.xaethos.android.halbrowser.fragment.BaseResourceFragment;
import net.xaethos.android.halbrowser.fragment.ResourceFragment;
import net.xaethos.android.halbrowser.fragment.URITemplateDialogFragment;
import net.xaethos.android.halparser.HALLink;
import net.xaethos.android.halparser.HALResource;

import java.net.URI;
import java.util.Map;

public class BrowserActivity extends FragmentActivity
        implements
ResourceFragment.OnLinkFollowListener,
        LoaderManager.LoaderCallbacks<HALResource>
{
    public static final String EXTRA_API_URL = "api_url";
    private HALResource mResource;
    private ResourceFragment mFragment;

    // *** Activity life-cycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadResourceFragment(null);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    // *** Options Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_reload);
        if (mResource != null) {
            HALLink link = mResource.getLink(Relation.SELF);
            menuItem.setEnabled(link != null);
        }
        else {
            menuItem.setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_reload:
            getSupportLoaderManager().restartLoader(0, null, this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // *** ResourceFragment.OnLinkFollowListener implementation

    @Override
    public void onFollowLink(HALLink link, Map<String, Object> map) {
        if (link == null) {
            Toast.makeText(this, "No link to follow", Toast.LENGTH_SHORT).show();
            return;
        }

        if (link.isTemplated()) {
            if (map == null) {
                URITemplateDialogFragment.forLink(link).show(getSupportFragmentManager(), "uritemplate");
            }
            else {
                followURI(link.getURI(map));
            }
            return;
        }

        if (link.getRel() == "external") {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getHref()));
            startActivity(intent);
            return;
        }

        followURI(link.getURI());
    }

    // *** Helper methods

    protected void followURI(URI target) {
        Intent intent = new Intent(getIntent());
        intent.setData(Uri.parse(target.toString()));
        startActivity(intent);
    }

    private void loadResourceFragment(HALResource resource) {
        FragmentManager manager = getSupportFragmentManager();

        mFragment = (ResourceFragment) manager.findFragmentById(android.R.id.content);
        if (mFragment == null) {
            ((ViewGroup) findViewById(android.R.id.content)).removeAllViews();

            mFragment = getResourceFragment(resource);

            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(android.R.id.content, (Fragment) mFragment);
            transaction.commit();
        }
    }

    private ResourceFragment getResourceFragment(HALResource resource) {
        return new BaseResourceFragment.Builder()
                .setResource(resource)
                .showBasicRels(true)
                .buildFragment(BaseResourceFragment.class);
    }

    // *** LoaderManager.LoaderCallbacks<HALResource>

    @Override
    public Loader<HALResource> onCreateLoader(int id, Bundle args) {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        APIClient client = new APIClient.Builder(intent.getStringExtra(EXTRA_API_URL)).build();

        if (uri != null) {
            return client.getLoaderForURI(this, uri.toString());
        }
        else {
            return client.getLoader(this);
        }
    }

    @Override
    public void onLoadFinished(Loader<HALResource> loader, HALResource resource) {
        mResource = resource;
        mFragment.setResource(resource);
        invalidateOptionsMenu();

        if (resource != null) {
            String title = resource.getLink(Relation.SELF).getTitle();
            if (!TextUtils.isEmpty(title)) {
                setTitle(title);
            }
        }
        else {
            Toast.makeText(BrowserActivity.this, "Couldn't GET relation :(", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onLoaderReset(Loader<HALResource> loader) {
        mResource = null;
        mFragment.setResource(null);
        invalidateOptionsMenu();
    }

}
