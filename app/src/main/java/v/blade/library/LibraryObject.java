package v.blade.library;

import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;

import v.blade.sources.SourceInformation;

public abstract class LibraryObject
{
    protected String name;
    protected ArrayList<SourceInformation> sources;
    protected RequestCreator imageRequest = null;
    protected String imageStr = null;

    public String getName()
    {
        return name;
    }

    public RequestCreator getImageRequest()
    {
        return imageRequest;
    }

    public void setImageRequest(RequestCreator request)
    {
        this.imageRequest = request;
    }

    public String getImageStr()
    {
        return imageStr;
    }
}
