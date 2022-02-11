package v.blade.ui;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface GeniusService
{
    class SearchResultObject
    {
        int annotation_count;
        String api_path;
        String artist_names;
        String full_title;
        long id;
        String lyrics_state;
        String title;
        String title_with_featured;
        String path;
    }

    class SearchHitObject
    {
        String index;
        String type;
        SearchResultObject result;
    }

    class SearchResponse
    {
        SearchHitObject[] hits;
    }

    class MetaObject
    {
        int status;
    }

    class SearchApiResponse
    {
        MetaObject meta;
        SearchResponse response;
    }


    @GET("search")
    Call<SearchApiResponse> search(@Header("Authorization") String authorization, @Query("q") String query);
}
