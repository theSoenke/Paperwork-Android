package rocks.paperwork.android.sync.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.android.adapters.Tag;

/**
 * Syncs tags with the server
 */
public class TagSync
{
    private static final String LOG_TAG = TagSync.class.getSimpleName();

    public static List<Tag> parseTags(String jsonStr) throws JSONException
    {
        List<Tag> tags = new ArrayList<>();

        JSONObject jsonData = new JSONObject(jsonStr);
        JSONArray jsonTags = jsonData.getJSONArray("response");

        for (int i = 0; i < jsonTags.length(); i++)
        {
            JSONObject tagJson = jsonTags.getJSONObject(i);
            String id = tagJson.getString("id");
            String title = tagJson.getString("title");

            Tag tag = new Tag(id);
            tag.setTitle(title);

            tags.add(tag);
        }

        return tags;
    }
}
