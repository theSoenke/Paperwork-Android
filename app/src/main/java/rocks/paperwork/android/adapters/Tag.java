package rocks.paperwork.android.adapters;

import java.io.Serializable;

public class Tag implements Serializable
{
    private final String mId;
    private String mTitle;

    public Tag(String id)
    {
        mId = id;
    }

    public String getId()
    {
        return mId;
    }

    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }
}
