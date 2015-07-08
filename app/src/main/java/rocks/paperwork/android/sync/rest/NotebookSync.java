package rocks.paperwork.android.sync.rest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.android.adapters.NotebookAdapter;

/**
 * Syncs notebooks with the server
 */
public class NotebookSync
{
    private static final String LOG_TAG = NotebookSync.class.getSimpleName();

    public static List<NotebookAdapter.Notebook> parseNotebooks(String jsonStr) throws JSONException
    {
        List<NotebookAdapter.Notebook> notebooks = new ArrayList<>();

        JSONObject jsonData = new JSONObject(jsonStr);
        JSONArray jsonNotebooks = jsonData.getJSONArray("response");

        for (int i = 0; i < jsonNotebooks.length(); i++)
        {
            JSONObject notebookJson = jsonNotebooks.getJSONObject(i);
            String id = notebookJson.getString("id");
            String title = notebookJson.getString("title");

            if (!id.equals(NotebookAdapter.Notebook.DEFAULT_ID))
            {
                NotebookAdapter.Notebook notebook = new NotebookAdapter.Notebook(id);
                notebook.setTitle(title);

                notebooks.add(notebook);
            }
        }

        return notebooks;
    }
}
