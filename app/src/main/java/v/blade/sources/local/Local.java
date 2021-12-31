package v.blade.sources.local;

import v.blade.R;
import v.blade.sources.Source;

public class Local extends Source
{
    public static int NAME_RESOURCE = R.string.local;
    public static int DESCRIPTION_RESOURCE = R.string.local_desc;
    public static int IMAGE_RESOURCE = R.drawable.ic_local;

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
    }
}
