package net.xaethos.tabby;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import net.xaethos.android.halbrowser.fragment.OnLinkFollowListener;
import net.xaethos.android.halbrowser.fragment.ProfileResourceFragment;
import net.xaethos.android.halbrowser.fragment.URITemplateDialogFragment;
import net.xaethos.android.halbrowser.profile.ProfileInflater;
import net.xaethos.android.halparser.HALLink;
import net.xaethos.android.halparser.HALResource;

import java.net.URI;
import java.util.Map;

public class MainActivity extends FragmentActivity
        implements
        OnLinkFollowListener,
        LoaderManager.LoaderCallbacks<HALResource>
{
    private static final String SELF = "self";
    private static final URI BASE_URI = URI.create("http://enigmatic-plateau-6595.herokuapp.com/articles");
    public static final int FRAGMENT_ID = android.R.id.content;

    private HALResource mResource;
    private ProfileResourceFragment mFragment;
    private ProfileInflater mProfileInflater = new ProfileInflater();

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
        if (menuItem != null) {
            if (mResource != null) {
                menuItem.setEnabled(mResource.getLink(SELF) != null);
            } else {
                menuItem.setEnabled(false);
            }
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

        if ("external".equals(link.getRel())) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.getHref()));
            startActivity(intent);
            return;
        }

        followURI(link.getURI());
    }

    // *** Helper methods

    protected void followURI(URI target) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target.toString()), this, this.getClass());
        startActivity(intent);
    }

    private void loadResourceFragment(HALResource resource) {
        FragmentManager manager = getSupportFragmentManager();
        mFragment = newResourceFragment(resource);
        FragmentTransaction transaction = manager.beginTransaction();
        if (manager.findFragmentById(FRAGMENT_ID) == null) {
            ((ViewGroup) findViewById(FRAGMENT_ID)).removeAllViews();
            transaction.add(FRAGMENT_ID, mFragment);
        } else {
            transaction.replace(FRAGMENT_ID, mFragment);
        }
        transaction.commit();
    }

    private ProfileResourceFragment newResourceFragment(HALResource resource) {
        ProfileResourceFragment fragment = new ProfileResourceFragment();
        fragment.setConfiguration(mProfileInflater.inflate(this, R.xml.default_profile));
        fragment.setResource(resource);
        return fragment;
    }

    // *** LoaderManager.LoaderCallbacks<HALResource>

    @Override
    public Loader<HALResource> onCreateLoader(int id, Bundle args) {
        APIClient client = new APIClient(BASE_URI);
        Uri uri = getIntent().getData();
        if (uri != null) {
            return client.getLoaderForURI(this, uri.toString());
        } else {
            return client.getLoader(this);
        }
    }

    @Override
    public void onLoadFinished(Loader<HALResource> loader, HALResource resource) {
        mResource = resource;
        mHandler.sendEmptyMessage(0);

        if (resource != null) {
            String title = resource.getLink(SELF).getTitle();
            if (!TextUtils.isEmpty(title)) {
                setTitle(title);
            }
        } else {
            Toast.makeText(MainActivity.this, "Couldn't GET relation :(", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onLoaderReset(Loader<HALResource> loader) {
        mResource = null;
        mHandler.sendEmptyMessage(0);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            loadResourceFragment(mResource);
            invalidateOptionsMenu();
            return true;
        }
    });

}
