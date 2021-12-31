package v.blade.sources;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public abstract class Source
{
    public static final ArrayList<Source> SOURCES = new ArrayList<>();

    public enum SourceStatus
    {
        STATUS_DOWN, //Down : not usable
        STATUS_NEED_INIT, //Need init : not yet initialized
        STATUS_CONNECTING, //Connecting : need to wait for connection
        STATUS_READY //Ready : source is ready and available for use
    }

    protected String name;
    protected SourceStatus status = SourceStatus.STATUS_NEED_INIT;
    protected Fragment settingsFragment = null;

    public Source()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public SourceStatus getStatus()
    {
        return this.status;
    }

    public void setStatus(SourceStatus status)
    {
        this.status = status;
    }

    public abstract int getImageResource();
}