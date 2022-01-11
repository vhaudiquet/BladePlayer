package v.blade;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webKeys;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import v.blade.sources.Source;
import v.blade.sources.spotify.Spotify;
import v.blade.ui.SettingsActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SpotifyConnectionTest
{
    /*
     * This is an Espresso test, testing that connecting to a Spotify account works as intended
     *
     * It is also useful to automate screenshots, as once Spotify connected it can populate the
     * library and thus we generate nice screenshots :))
     */

    @Rule
    public ActivityScenarioRule<SettingsActivity> settingsActivityRule = new ActivityScenarioRule<>(SettingsActivity.class);

    @Before
    public void setup()
    {

    }

    private int getSpotifyAmount()
    {
        int amount = 0;
        for(Source source : Source.SOURCES)
            if(source instanceof Spotify)
                amount++;
        return amount;
    }

    @Test
    public void addSpotifyConnection() throws InterruptedException
    {
        //Check Spotify amount
        int beforeSpotifyAmount = getSpotifyAmount();

        //Go to source
        onView(withText(R.string.sources)).perform(click());
        //Click on <add source> button
        onView(withId(R.id.settings_source_add)).perform(click());
        //Click on 'Spotify'
        onView(withText(R.string.spotify)).perform(click());

        //Check if Spotify source was added
        int afterSpotifyAmount = getSpotifyAmount();
        assert afterSpotifyAmount == beforeSpotifyAmount + 1;

        //Open the added Spotify settings fragment
        int newSpotifyIndex = Source.SOURCES.size() - 1;
        onView(withId(R.id.settings_sources_listview)).perform(RecyclerViewActions
                .actionOnItemAtPosition(newSpotifyIndex, click()));

        //Fill login informations
        String spotifyUser = InstrumentationRegistry.getArguments().getString("spotify_user");
        String spotifyPass = InstrumentationRegistry.getArguments().getString("spotify_password");
        onView(withId(R.id.settings_spotify_user)).perform(typeText(spotifyUser));
        closeSoftKeyboard();
        onView(withId(R.id.settings_spotify_password)).perform(typeText(spotifyPass));
        closeSoftKeyboard();

        //Start connection procedure
        onView(withId(R.id.settings_spotify_sign_in)).perform(click());

        /* In theory, there are now multiple cases :
         *  - The user does not have Spotify application installed
         *    - It's first launch : enter credentials (again) and accept
         *    - Authcode accepted before : login is done after a short amount of time
         * - The user does have Spotify app installed :
         *   - It's first launch : accept
         *   - Authcode accepted before : login is done after a (not so short) amount of time
         *
         *  We will handle only 'no spotify app' case for now
         */

        //Wait for window showup
        boolean windowOn = false;
        while(!windowOn)
        {
            try
            {
                onWebView(withResourceName("com_spotify_sdk_login_webview"))
                        .withElement(findElement(Locator.ID, "login-username"))
                        .perform(webKeys(spotifyUser));
                closeSoftKeyboard();
                windowOn = true;
            }
            catch(RuntimeException e)
            {
                Thread.sleep(1000);
            }
        }

        //Fill webview login infos and click on connect
        onWebView(withResourceName("com_spotify_sdk_login_webview"))
                .withElement(findElement(Locator.ID, "login-password"))
                .perform(webKeys(spotifyPass));
        closeSoftKeyboard();

        onWebView(withResourceName("com_spotify_sdk_login_webview"))
                .withElement(findElement(Locator.ID, "login-button"))
                .perform(webClick());

        //Now there are 2 cases : either login is done, or webview is still here
        // and we have to click accept
        try
        {
            onWebView(withResourceName("com_spotify_sdk_login_webview"))
                    .withElement(findElement(Locator.ID, "auth-accept"))
                    .perform(webClick());
        }
        catch(RuntimeException ignored)
        {
        }

        Thread.sleep(1000);

        //Done : check that it went well
        assert Source.SOURCES.get(newSpotifyIndex).getStatus() == Source.SourceStatus.STATUS_READY;
    }
}
