package rocks.paperwork.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import rocks.paperwork.R;
import rocks.paperwork.data.HostPreferences;
import rocks.paperwork.data.NotesDataSource;
import rocks.paperwork.fragments.NotebooksFragment;
import rocks.paperwork.fragments.NotesFragment;
import rocks.paperwork.interfaces.AsyncCallback;
import rocks.paperwork.network.SyncNotesTask;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AsyncCallback
{
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static MainActivity sInstance;
    private NavigationView mNavigationView;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private int mCurrentSelectedPosition;
    private boolean mUserLearnedDrawer;
    private SubMenu mTagMenu;

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
    protected void onRestoreInstanceState(Bundle savedInstanceState)
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

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        new SyncNotesTask(this).fetchAllData();
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        if (!mNavigationView.isShown())
        {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
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
        if (id == R.id.action_search)
        {
            return true;
        }
        else if (id == R.id.action_logout)
        {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void logout()
    {
        HostPreferences.clearPreferences(this);
        NotesDataSource.getInstance(this).deleteAll();
        Intent hostMenuIntent = new Intent(this, LoginActivity.class);
        startActivity(hostMenuIntent);
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
            case R.id.nav_settings:
                break;
            case R.id.nav_about:
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
        // TODO put in the right place and set tag icons

        /*

        NotesDataSource notesDataSource = NotesDataSource.getInstance(this);
        List<Tag> tags = notesDataSource.getAllTags();

        if (mTagMenu == null)
        {
            Menu menu = mNavigationView.getMenu();
            mTagMenu = menu.addSubMenu(R.string.tags);
        }

        mTagMenu.clear();

        for (Tag tag : tags)
        {
            mTagMenu.add(tag.getTitle());
        }*/
    }
}
