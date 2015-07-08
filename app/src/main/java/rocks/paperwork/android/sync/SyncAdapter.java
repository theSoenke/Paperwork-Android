package rocks.paperwork.android.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import rocks.paperwork.android.R;
import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.adapters.NotesAdapter.Note;
import rocks.paperwork.android.adapters.Tag;
import rocks.paperwork.android.data.HostPreferences;
import rocks.paperwork.android.data.NoteDataSource;
import rocks.paperwork.android.sync.rest.FetchTask;
import rocks.paperwork.android.sync.rest.NoteSync;
import rocks.paperwork.android.sync.rest.NotebookSync;
import rocks.paperwork.android.sync.rest.TagSync;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private static final String LOG_TAG = SyncAdapter.class.getSimpleName();
    private static SwipeRefreshLayout sSwipeContainer;

    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
    }

    private enum NoteData
    {
        notes,
        notebooks,
        tags
    }

    public static void syncImmediately(Context context)
    {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static void syncImmediately(Context context, SwipeRefreshLayout refreshLayout)
    {
        sSwipeContainer = refreshLayout;

        if (!isNetworkAvailable(context))
        {
            sSwipeContainer.setRefreshing(false);
            Log.d(LOG_TAG, "No internet available");
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    // TODO use paperwork account instead of fake account
    public static Account getSyncAccount(Context context)
    {
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount))
        {
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null))
            {
                return null;
            }
        }
        return newAccount;
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult)
    {
        Log.d(LOG_TAG, "onPerformSync Called.");

        String host = HostPreferences.readSharedSetting(getContext(), HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(getContext(), HostPreferences.HASH, "");

        if (!HostPreferences.preferencesExist(getContext()))
        {
            Log.d(LOG_TAG, "User is not logged in");
            return;
        }

        try
        {
            NoteSync.uploadNotes(getContext(), host, hash);
            NoteSync.deleteNotes(getContext(), host, hash);
            fetch(host, hash, NoteData.notebooks);
            fetch(host, hash, NoteData.notes);
            fetch(host, hash, NoteData.tags);
        }
        catch (JSONException e)
        {
            Log.d(LOG_TAG, "JSONException", e);
        }
        catch (AuthenticatorException e)
        {
            Log.d(LOG_TAG, "Authentication failed");
            authenticationFailed();
        }
        catch (FileNotFoundException e)
        {
            // FIXME workaround because response code is always 200, even if authentication failed
            authenticationFailed();
        }
        catch (IOException e)
        {
            Log.d(LOG_TAG, "IOException", e);
        }
        finally
        {
            if (sSwipeContainer != null)
            {
                sSwipeContainer.setRefreshing(false);
            }
            Log.d(LOG_TAG, "Error syncing data");
        }
    }

    private void fetch(String host, String hash, NoteData data) throws IOException, JSONException, AuthenticatorException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(getContext());
        SyncManager syncManager = new SyncManager(getContext());

        if (data == NoteData.notes)
        {
            String result = FetchTask.fetchData(host + "/api/v1/notebooks/" + Notebook.DEFAULT_ID + "/notes", hash);
            List<Note> remoteNotes = NoteSync.parseNotes(result);
            List<Note> localNotes = dataSource.getNotes(null);

            Pair<List<Note>, List<Note>> syncedNotes = syncManager.syncNotes(localNotes, remoteNotes);
            dataSource.bulkInsertNotes(syncedNotes.first);
            NoteSync.updateNotes(getContext(), host, hash, syncedNotes.second);
        }
        else if (data == NoteData.notebooks)
        {
            String result = FetchTask.fetchData(host + "/api/v1/notebooks/", hash);
            List<Notebook> remoteNotebooks = NotebookSync.parseNotebooks(result);
            NoteDataSource.getInstance(getContext()).bulkInsertNotebooks(remoteNotebooks);
        }
        else if (data == NoteData.tags)
        {
            String result = FetchTask.fetchData(host + "/api/v1/tags/", hash);
            List<Tag> remoteTags = TagSync.parseTags(result);
            NoteDataSource.getInstance(getContext()).bulkInsertTags(remoteTags);
        }
    }

    private void authenticationFailed()
    {
        // TODO handle authentication failure
        Log.d(LOG_TAG, "Authentication failed");
    }

    private static boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
}