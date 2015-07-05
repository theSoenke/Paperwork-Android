package rocks.paperwork.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.UUID;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.data.DatabaseContract;
import rocks.paperwork.data.DatabaseHelper;
import rocks.paperwork.data.NoteDataSource;
import rocks.paperwork.sync.SyncAdapter;

public class NoteActivity extends AppCompatActivity
{
    private final String LOG_TAG = NoteActivity.class.getName();
    private Toolbar mToolbar;
    private Note mNote;
    private EditText mTextTitle;
    private EditText mEditContent;
    private boolean mNewNote;
    private boolean mEditMode;
    private FloatingActionButton mEditNoteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mTextTitle = (EditText) findViewById(R.id.note_title);
        mEditContent = (EditText) findViewById(R.id.note_edit_content);
        mEditNoteButton = (FloatingActionButton) findViewById(R.id.edit_note);

        mEditNoteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setMode(true);
            }
        });

        if (getIntent().hasExtra("NOTES"))
        {
            mNote = (NotesAdapter.Note) getIntent().getExtras().getSerializable("NOTES");
            mTextTitle.setText(mNote.getTitle());

            String content = mNote.getContent();
            mEditContent.setText(Html.fromHtml(content));
        }
        else
        {
            mNewNote = true;
        }

        if (getIntent().hasExtra("EditMode"))
        {
            mEditMode = getIntent().getExtras().getBoolean("EditMode");
        }
        else
        {
            Log.e(LOG_TAG, "EditMode is not set");
        }

        setMode(mEditMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolbar_text));
        }
    }

    /**
     * @param editMode enables or disables edit mode for the note
     */
    private void setMode(boolean editMode)
    {
        mEditNoteButton.setVisibility(editMode ? View.GONE : View.VISIBLE);

        if (editMode)
        {
            mEditContent.requestFocus();
            showKeyboard();

            mEditContent.setFocusableInTouchMode(true);
            mEditContent.setCursorVisible(true);
            mTextTitle.setFocusableInTouchMode(true);
            mTextTitle.setCursorVisible(true);
            mToolbar.setNavigationIcon(R.drawable.ic_done_grey);
        }
        else
        {
            hideKeyboard();

            mEditContent.setFocusable(false);
            mEditContent.setCursorVisible(false);

            mTextTitle.setFocusable(false);
            mTextTitle.setCursorVisible(false);
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_grey);
        }

        mEditMode = editMode;
    }

    private void showKeyboard()
    {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideKeyboard()
    {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(mEditContent.getWindowToken(), 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home)
        {
            if (mEditMode)
            {
                String title = mTextTitle.getText().toString();

                if (title.isEmpty())
                {
                    mTextTitle.setError(getString(R.string.empty_title_error));
                    return false;
                }
            }

            if (mEditMode && !mNewNote)
            {
                setNoteResult();
                setMode(false);
            }
            else if (mEditMode && mNewNote)
            {
                setNoteResult();
                onBackPressed();
            }
            else
            {
                onBackPressed();
            }

            return true;
        }
        else if (id == R.id.action_delete)
        {
            if (mNewNote)
            {
                onBackPressed();
                finish();
            }
            else
            {
                showDeleteNoteDialog();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause()
    {
        hideKeyboard();
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        hideKeyboard();
        super.onStop();
    }

    private boolean changesToSave()
    {
        if (!mEditMode)
        {
            return false;
        }

        String title = mTextTitle.getText().toString();
        String content = mEditContent.getText().toString();

        if (mNote == null) // note does not exist yet
        {
            if (!content.isEmpty() || title.isEmpty())
            {
                return true;
            }
        }
        else
        {
            if (!content.equals(Html.fromHtml(mNote.getContent()).toString()))
            {
                return true;
            }
            else if (!title.equals(mNote.getTitle()))
            {
                return true;
            }
        }
        return false;
    }

    private void setNoteResult()
    {
        if (changesToSave())
        {
            if (mNewNote)
            {
                mNote = new Note(UUID.randomUUID().toString());
                mNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.not_synced);
                String notebookId = (String) getIntent().getExtras().getSerializable("NotebookId");
                mNote.setNotebookId(notebookId);
            }
            else
            {
                if (mNote.getSyncStatus() != DatabaseContract.NoteEntry.NOTE_STATUS.not_synced)
                {
                    mNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.edited);
                }
            }
            mNote.setTitle(mTextTitle.getText().toString());
            mNote.setContent(Html.toHtml(mEditContent.getText()));
            mNote.setUpdatedAt(DatabaseHelper.getCurrentTime());

            NoteDataSource.getInstance(this).insertNote(mNote);

            SyncAdapter.syncImmediately(NoteActivity.this);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mEditMode)
        {
            if (!changesToSave() && mNewNote)
            {
                super.onBackPressed();
            }
            setMode(false);
        }
        else if (changesToSave())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.discard).setMessage(R.string.discard_changes)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            back();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {

                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else
        {
            super.onBackPressed();
        }
    }

    private void back()
    {
        super.onBackPressed();
    }

    private void showDeleteNoteDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
        builder.setTitle(getString(R.string.delete) + ": " + mNote.getTitle())
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.deleted);
                        NoteDataSource.getInstance(NoteActivity.this).insertNote(mNote);
                        SyncAdapter.syncImmediately(NoteActivity.this);
                        onBackPressed();
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
