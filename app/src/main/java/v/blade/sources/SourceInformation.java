package v.blade.sources;

public class SourceInformation
{
    public Source source;
    public Object id;
    //Whether or not this is a 'handle' for the song, i.e. not in source library
    public boolean handled;

    public SourceInformation(Source source, Object id, boolean handled)
    {
        this.source = source;
        this.id = id;
        this.handled = handled;
    }
}
