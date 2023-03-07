/* Copyright 2022 Valentin HAUDIQUET

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package v.blade;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import v.blade.library.Library;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;

public class BladeApplication extends Application
{
    public static abstract class Callback<T>
    {
        public abstract void run(T arg0);
    }

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static final ExecutorService executorService = threadPoolExecutor;
    public static Context appContext;
    public static boolean shouldDisplayFirstLaunchDialog = false;

    @SuppressLint("StaticFieldLeak")
    public static Activity currentActivity = null;

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
        {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle)
            {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity)
            {
                currentActivity = activity;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity)
            {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity)
            {
                if(currentActivity == activity)
                    currentActivity = null;

                // NOTE: This happens even when switching between Main/Play activities
                // TODO: Maybe find a better place to save playlist ? (this will cost disk usage...)
                // (it's not as bad as it seems, we have kernel cache...)
                System.out.println("BLADE: onActivityPaused....");
                if(MediaBrowserService.getInstance() != null)
                    MediaBrowserService.getInstance().savePlaylist();
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity)
            {
                if(currentActivity == activity)
                    currentActivity = null;
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle)
            {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity)
            {
                if(currentActivity == activity)
                    currentActivity = null;
            }
        });

        //Restore theme from preferences
        String dark_theme = PreferenceManager.getDefaultSharedPreferences(base).getString("dark_theme", "system_default");
        switch(dark_theme)
        {
            case "system_default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "dark_theme":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "light_theme":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }

        //Provide static access to application context (eg. for 'Local' source, needing ContentProvider)
        appContext = base;

        //Load sources
        executorService.execute(() ->
        {
            Source.loadSourcesFromSave();
            Source.initSources();

            // Bind MediaBrowserService to application
            Intent serviceIntent = new Intent(this, MediaBrowserService.class);
            bindService(serviceIntent, new ServiceConnection()
            {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service)
                {
                }

                @Override
                public void onServiceDisconnected(ComponentName name)
                {
                }
            }, 0);

            Library.loadFromCache();

            if(Source.SOURCES.size() == 0)
                shouldDisplayFirstLaunchDialog = true;
        });
    }

    public static ExecutorService obtainExecutorService()
    {
        return executorService;
    }
}
