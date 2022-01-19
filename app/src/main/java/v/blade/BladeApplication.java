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

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import v.blade.library.Library;
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

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

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
