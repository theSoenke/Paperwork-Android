package rocks.paperwork.android.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import rocks.paperwork.android.R;
import rocks.paperwork.android.adapters.Tag;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.HostPreferences;
import rocks.paperwork.android.data.NoteDataSource;
import rocks.paperwork.android.fragments.NotebooksFragment;
import rocks.paperwork.android.fragments.NotesFragment;
import rocks.paperwork.android.interfaces.AsyncCallback;
import rocks.paperwork.android.sync.SyncAdapter;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AsyncCallback
{
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static MainActivity sInstance;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private int mCurrentSelectedPosition;
    private boolean mUserLearnedDrawer;
    private SubMenu mTagMenu;
    private NavigationView mNavigationView;

    public static MainActivity getInstance()
    {
        return sInstance;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        sInstance = this;

        if (!HostPreferences.preferencesExist(this))
        {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.nav_drawer);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mUserLearnedDrawer = Boolean.valueOf(HostPreferences.readSharedSetting(this, HostPreferences.PREF_USER_LEARNED_DRAWER, "false"));
        mNavigationView.setNavigationItemSelectedListener(this);

        String email = HostPreferences.readSharedSetting(this, "email", "");
        TextView userEmail = (TextView) findViewById(R.id.user_email);
        userEmail.setText(email);

        setUpToolbar();
        setUpNavDrawer();

        if (savedInstanceState != null)
        {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }

        Menu menu = mNavigationView.getMenu();
        menu.getItem(mCurrentSelectedPosition).setChecked(true);
        onNavigationItemSelected(menu.getItem(mCurrentSelectedPosition));

        mTagMenu = menu.addSubMenu(R.string.tags);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        getContentResolver().registerContentObserver(
                DatabaseContract.NoteEntry.CONTENT_URI, true, new ContentObserver(new Handler(getMainLooper()))
                {
                    @Override
                    public void onChange(boolean selfChange)
                    {
                        updateView();
                    }
                });


        SyncAdapter.syncImmediately(this);
        updateView();
    }

    private void setUpToolbar()
    {
        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (mToolbar != null)
        {
            setSupportActionBar(mToolbar);
        }
    }

    private void setUpNavDrawer()
    {
        if (mToolbar != null)
        {
            if (getSupportActionBar() != null)
            {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            mToolbar.setNavigationOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (!mUserLearnedDrawer)
        {
            mDrawerLayout.openDrawer(GravityCompat.START);
            mUserLearnedDrawer = true;
            HostPreferences.saveSharedSetting(this, HostPreferences.PREF_USER_LEARNED_DRAWER, "true");
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout)
        {
            logout();
            return true;
        }
        else if (id == R.id.action_search)
        {
            Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(searchIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void logout()
    {
        HostPreferences.clearPreferences(this);
        NoteDataSource.getInstance(this).deleteAll();
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem)
    {
        menuItem.setChecked(true);
        Fragment fragment;
        FragmentManager fm = getFragmentManager();

        switch (menuItem.getItemId())
        {
            case R.id.nav_all_notes:
                setTitle(getString(R.string.all_notes));
                fragment = Fragment.instantiate(MainActivity.this, NotesFragment.class.getName());
                (fm.beginTransaction().replace(R.id.main_container, fragment)).commit();
                mCurrentSelectedPosition = 0;
                break;
            case R.id.nav_notebooks:
                setTitle(getString(R.string.notebooks));
                fragment = Fragment.instantiate(MainActivity.this, NotebooksFragment.class.getName());
                (fm.beginTransaction().replace(R.id.main_container, fragment)).commit();
                mCurrentSelectedPosition = 1;
                break;
            default:
                return true;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void updateView()
    {
        NoteDataSource notesDataSource = NoteDataSource.getInstance(this);
        List<Tag> tags = notesDataSource.getAllTags();

        mTagMenu.clear();

        for (final Tag tag : tags)
        {
            final MenuItem menuItem = mTagMenu.add(tag.getTitle());
            menuItem.setIcon(R.mipmap.ic_tags_grey);

            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    setTitle(tag.getTitle());
                    NotesFragment noteFragment = (NotesFragment) Fragment.instantiate(MainActivity.this, NotesFragment.class.getName());
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(NotesFragment.KEY_TAG, tag);
                    noteFragment.setArguments(bundle);
                    (getFragmentManager().beginTransaction().replace(R.id.main_container, noteFragment)).commit();
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            });
        }

        // workaround for bug in navgationview where it's not refreshed after a new item is added
        // https://code.google.com/p/android/issues/detail?id=176300
        for (int i = 0, count = mNavigationView.getChildCount(); i < count; i++)
        {
            final View child = mNavigationView.getChildAt(i);
            if (child != null && child instanceof ListView)
            {
                final ListView menuView = (ListView) child;
                final HeaderViewListAdapter adapter = (HeaderViewListAdapter) menuView.getAdapter();
                final BaseAdapter wrapped = (BaseAdapter) adapter.getWrappedAdapter();
                wrapped.notifyDataSetChanged();
            }
        }
    }
}
