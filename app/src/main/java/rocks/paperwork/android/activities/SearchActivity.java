package rocks.paperwork.android.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import rocks.paperwork.android.R;
import rocks.paperwork.android.fragments.NotesFragment;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener
{
    private NotesFragment mNotesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mNotesFragment = (NotesFragment) Fragment.instantiate(SearchActivity.this, NotesFragment.class.getName());
        Bundle bundle = new Bundle();
        bundle.putBoolean(NotesFragment.KEY_SEARCH_MODE, true);
        mNotesFragment.setArguments(bundle);
        (getFragmentManager().beginTransaction().replace(R.id.main_container, mNotesFragment)).commit();

        SearchView searchView = (SearchView) findViewById(R.id.search);
        searchView.onActionViewExpanded();
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
        {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query)
    {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        mNotesFragment.searchNotes(newText);
        return false;
    }
}
