package v.blade.sources.spotify;

import com.google.gson.annotations.SerializedName;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The Retrofit service allowing Spotify Web API access
 */
public interface SpotifyService
{
    class AlbumRestrictionObject
    {
        String reason;
    }

    class CopyrightObject
    {
        String text;
        String type;
    }

    class ErrorObject
    {
        String message;
        int status;
    }

    class ExplicitContentSettingsObject
    {
        boolean filter_enabled;
        boolean filter_locked;
    }

    class ExternalIdObject
    {
        String ean;
        String isrc;
        String upc;
    }

    class ExternalUrlObject
    {
        String spotify;
    }

    class FollowersObject
    {
        String href;
        int total;
    }

    class ImageObject
    {
        int height;
        String url;
        int width;
    }

    class PagingObject<T>
    {
        String href;
        T[] items;
        int limit;
        String next;
        int offset;
        String previous;
        int total;
    }

    class PlaylistObject
    {
        boolean collaborative;
        String description;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String name;
        PublicUserObject owner;
        @SerializedName("public")
        boolean is_public;
        String snapshot_id;
        PlaylistTrackObject[] tracks;
        String type;
        String uri;
    }

    class PlaylistTrackObject
    {
        String added_at; //Timestamp ?
        PublicUserObject added_by;
        boolean is_local;
        TrackObject track;
    }

    class PlaylistTracksRefObject
    {
        String href;
        int total;
    }

    class PrivateUserObject
    {
        String country;
        String display_name;
        String email;
        ExplicitContentSettingsObject explicit_content;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String product;
        String type;
        String uri;
    }

    class PublicUserObject
    {
        String display_name;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String type;
        String uri;
    }

    class SavedAlbumObject
    {
        String added_at; //Timestamp
        AlbumObject album;
    }

    class SavedTrackObject
    {
        String added_at; //Timestamp
        TrackObject track;
    }

    class SimplifiedAlbumObject
    {
        String album_group;
        String album_type;
        SimplifiedArtistObject[] artists;
        String[] available_markets;
        ExternalUrlObject external_urls;
        String href;
        String id;
        ImageObject[] images;
        String name;
        String release_date;
        String release_date_precision;
        AlbumRestrictionObject restrictions;
        String type;
        String uri;
    }

    class AlbumObject extends SimplifiedAlbumObject
    {
        //ArtistObject[] artists;
        CopyrightObject[] copyrights;
        ExternalIdObject external_ids;
        String[] genres;
        String label;
        int popularity;
        PagingObject<SimplifiedTrackObject> tracks;
    }

    class SimplifiedArtistObject
    {
        ExternalUrlObject external_urls;
        String href;
        String id;
        String name;
        String type;
        String uri;
    }

    class ArtistObject extends SimplifiedArtistObject
    {
        FollowersObject followers;
        String[] genres;
        ImageObject[] images;
        int popularity;
    }


    class SimplifiedPlaylistObject
    {
        boolean collaborative;
        String description;
        ExternalUrlObject external_urls;
        String href;
        String id;
        ImageObject[] images;
        String name;
        PublicUserObject owner;
        @SerializedName("public")
        boolean is_public;
        String snapshot_id;
        PlaylistTracksRefObject tracks;
        String type;
        String uri;
    }

    class SimplifiedTrackObject
    {
        SimplifiedArtistObject[] artists;
        String[] available_markets;
        int disc_number;
        int duration_ms;
        boolean explicit;
        ExternalUrlObject external_urls;
        String href;
        String id;
        boolean is_local;
        boolean is_playable;
        //linked_from
        String name;
        String preview_url;
        TrackRestrictionObject restrictions;
        int track_number;
        String type;
        String uri;
    }

    class TrackObject extends SimplifiedTrackObject
    {
        SimplifiedAlbumObject album;
        //ArtistObject[] artists;
        ExternalIdObject external_ids;
        //linked_from
        int popularity;
    }

    class TrackRestrictionObject
    {
        String reason;
    }

    class UserInformationObject
    {
        static class ExplicitContent
        {
            boolean filter_enabled;
            boolean filter_locked;
        }

        static class Followers
        {
            String href;
            int total;
        }

        String country;
        String display_name;
        String email;
        ExplicitContent explicit_content;
        ExternalUrlObject external_urls;
        Followers followers;
        String href;
        String id;
        ImageObject[] images;
        String product;
        String type;
        String uri;
    }

    class PlaylistAddResponse
    {
        String snapshot_id;
    }

    class SearchResult
    {
        PagingObject<TrackObject> tracks;
        PagingObject<ArtistObject> artists;
        PagingObject<SimplifiedAlbumObject> albums;
    }

    class FeaturedPlaylistsResult
    {
        PagingObject<SimplifiedPlaylistObject> playlists;
        String message;
    }

    /**
     * max limit is 50
     */
    @GET("me/playlists")
    Call<PagingObject<SimplifiedPlaylistObject>> getListOfCurrentUserPlaylists(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 100
     */
    @GET("playlists/{playlist_id}/tracks")
    Call<PagingObject<PlaylistTrackObject>> getPlaylistItems(@Header("Authorization") String token, @Path("playlist_id") String playlist_id, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 50
     */
    @GET("me/tracks")
    Call<PagingObject<SavedTrackObject>> getUserSavedTracks(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 50
     */
    @GET("me/albums")
    Call<PagingObject<SavedAlbumObject>> getUserSavedAlbums(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    @GET("me")
    Call<UserInformationObject> getUser(@Header("Authorization") String token);

    @POST("playlists/{playlist_id}/tracks")
    Call<PlaylistAddResponse> appendTrackToPlaylist(@Header("Authorization") String token, @Path("playlist_id") String playlist_id, @Query("uris") String uris);

    @POST("users/{user_id}/playlists")
    Call<SimplifiedPlaylistObject> createPlaylist(@Header("Authorization") String token, @Path("user_id") String user_id, @Body RequestBody params);

    @DELETE("playlists/{playlist_id}/followers")
    Call<Void> unfollowPlaylist(@Header("Authorization") String token, @Path("playlist_id") String playlist_id);

    @PUT("me/tracks")
    Call<Void> saveTrack(@Header("Authorization") String token, @Query("ids") String id);

    @DELETE("me/tracks")
    Call<Void> removeTrack(@Header("Authorization") String token, @Query("ids") String id);

    @HTTP(method = "DELETE", path = "playlists/{playlist_id}/tracks", hasBody = true)
    Call<PlaylistAddResponse> removePlaylistItem(@Header("Authorization") String token, @Path("playlist_id") String playlist_id, @Body RequestBody params);

    /**
     * @param retType should be "track,artist,album"
     * @param limit   max limit is 50
     */
    @GET("search")
    Call<SearchResult> search(@Header("Authorization") String token, @Query("q") String query, @Query("type") String retType, @Query("limit") int limit);

    /**
     * @param limit max limit is 50
     */
    @GET("albums/{id}/tracks")
    Call<PagingObject<SimplifiedTrackObject>> getAlbumTracks(@Header("Authorization") String token, @Path("id") String id, @Query("limit") int limit);

    /**
     * @param limit max limit is 50
     */
    @GET("artists/{id}/albums")
    Call<PagingObject<SimplifiedAlbumObject>> getArtistAlbums(@Header("Authorization") String token, @Path("id") String id, @Query("limit") int limit);

    @GET("browse/featured-playlists")
    Call<FeaturedPlaylistsResult> getFeaturedPlaylists(@Header("Authorization") String token);
}
