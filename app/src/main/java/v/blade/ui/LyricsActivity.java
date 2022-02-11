package v.blade.ui;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;

public class LyricsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);

        WebView webView = findViewById(R.id.lyrics_webview);

        Song song = MediaBrowserService.getInstance().getPlaylist().get(MediaBrowserService.getInstance().getIndex());
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.genius.com")
                    .addConverterFactory(GsonConverterFactory.create()).build();
            GeniusService service = retrofit.create(GeniusService.class);

            Call<GeniusService.SearchApiResponse> search =
                    service.search("Bearer wTGF45NZElaOrhC1LIEhdBq9ISwX7SgNLBkp_74fjUo-uwUJNrENnCJ2Uj4tJeVo",
                            song.getName() + " " + song.getArtists()[0].getName());


            try
            {
                Response<GeniusService.SearchApiResponse> response = search.execute();

                if(response.code() != 200 || response.body() == null)
                {
                    return;
                }

                runOnUiThread(() ->
                        webView.loadUrl("https://genius.com" + response.body().response.hits[0].result.path));
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        });
    }
}