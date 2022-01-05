package v.blade.sources;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import v.blade.BladeApplication;
import v.blade.library.Library;

public abstract class Source
{
    private static final String SOURCES_FILE = "/sources.json";
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

    /**
     * Initialize source (connecting to account, server, ...)
     */
    public void initSource()
    {
        this.status = SourceStatus.STATUS_READY;
    }

    /**
     * Action done on 'synchronize' button ; as the Blade model is 'offline',
     * this is basically 'online register library' ; we do cache the library after
     */
    public abstract void synchronizeLibrary();

    public abstract Fragment getSettingsFragment();

    public abstract JsonObject saveToJSON();

    public abstract void restoreFromJSON(JsonObject jsonObject);

    /**
     * Blade saves all sources informations/configurations in a cache sources json file
     */
    public static synchronized void saveSources()
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            //Generate JSON Array of sources
            JsonArray sourceArray = new JsonArray();
            for(Source s : SOURCES)
            {
                sourceArray.add(s.saveToJSON());
            }

            //Write JSON Array to file
            File sourcesFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + SOURCES_FILE);
            try
            {
                //noinspection ResultOfMethodCallIgnored
                sourcesFile.delete();
                if(!sourcesFile.createNewFile())
                {
                    System.err.println("Could not save sources : could not create file");
                    return;
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(sourcesFile));
                Gson gson = new Gson();
                writer.write(gson.toJson(sourceArray));
                writer.close();
            }
            catch(IOException ignored)
            {
            }
        });
    }

    public static void loadSourcesFromSave()
    {
        File sourcesFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + SOURCES_FILE);
        try
        {
            //read file
            BufferedReader reader = new BufferedReader(new FileReader(sourcesFile));
            StringBuilder j = new StringBuilder();
            while(reader.ready()) j.append(reader.readLine()).append("\n");
            reader.close();

            //obtain from JSON
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Source.class, new SourceAdapter());
            Gson gson = gsonBuilder.create();
            JSONArray sourceArray = new JSONArray(j.toString());

            for(int i = 0; i < sourceArray.length(); i++)
            {
                Source s = gson.fromJson(sourceArray.get(i).toString(), Source.class);
                SOURCES.add(s);
            }
        }
        catch(IOException | JSONException ignored)
        {
        }
    }

    @SuppressWarnings("rawtypes")
    public static void synchronizeSources()
    {
        List<Future> futures = new ArrayList<>();
        for(Source s : SOURCES)
        {
            if(s.status != SourceStatus.STATUS_READY) continue;

            futures.add(BladeApplication.obtainExecutorService().submit(s::synchronizeLibrary));
        }

        BladeApplication.obtainExecutorService().execute(() ->
        {
            boolean allDone = false;
            while(!allDone)
            {
                allDone = true;
                for(Future f : futures) if(!f.isDone()) allDone = false;
            }

            //Every source synchronization is done, we can now sort and save library
            Library.generateLists();
        });
    }

    public static void initSources()
    {
        for(Source s : SOURCES)
            s.initSource();
    }

    protected static class SourceAdapter implements JsonDeserializer<Source>
    {
        @Override
        public Source deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject sourceObject = json.getAsJsonObject();
            String sourceClass = sourceObject.get("class").getAsString();

            try
            {
                //noinspection unchecked
                Class<? extends Source> c = (Class<? extends Source>) Class.forName(sourceClass);
                Source s = c.newInstance();
                s.restoreFromJSON(sourceObject);

                return s;
            }
            catch(ClassNotFoundException | IllegalAccessException | InstantiationException ignored)
            {
            }
            return null;
        }
    }
}