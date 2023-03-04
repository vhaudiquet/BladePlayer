package v.blade.sources.deezer;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The Retrofit service allowing Deezer Web API access
 */
public interface DeezerService
{
    class AlbumObject
    {
        long id;
        String title;
        String upc;
        String link;
        String share;
        String cover;
        String cover_small;
        String cover_medium;
        String cover_big;
        String cover_xl;
        String md5_image;
        int genre_id;
        GenreListObject genres;
        String label;
        int nb_tracks;
        int duration;
        int fans;
        String releaseDate;
        String record_type;
        boolean available;
        // alternative : Alternative object if not available...
        String tracklist;
        boolean explicit_lyrics;
        int explicit_content_lyrics;
        int explicit_content_cover;
        ContributorObject[] contributors;
        SimpleArtistObjectWithPicture artist;
        AlbumTracklistObject tracks;
    }

    class AlbumTracklistObject
    {
        SimpleTrackObject[] data;
    }

    class GenreListObject
    {
        SimpleGenreObject[] data;
    }

    class SimpleGenreObject
    {
        long id;
        String name;
        String picture;
        String type;
    }

    class GenreObject
    {
        long id;
        String name;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
    }

    class ContributorObject
    {
        long id;
        String name;
        String link;
        String share;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
        boolean radio;
        String tracklist;
        String type;
        String role;
    }

    class SimpleArtistObjectWithPicture
    {
        long id;
        String name;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
    }

    class SimpleArtistObject
    {
        long id;
        String name;
    }

    class SimpleAlbumObjectWithPicture
    {
        long id;
        String title;
        String cover;
        String cover_small;
        String cover_medium;
        String cover_big;
        String cover_xl;
        String type;
    }

    class SimpleTrackObject
    {
        long id;
        boolean readable;
        String title;
        String title_short;
        String title_version;
        String link;
        int duration;
        int rank;
        boolean explicit_lyrics;
        String preview;
        SimpleArtistObject artist;
        SimpleAlbumObjectWithPicture album;
        String type;
    }

    class ArtistObject
    {
        long id;
        String name;
        String link;
        String share;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
        int nb_album;
        int nb_fan;
        boolean radio;
        String tracklist;
    }

    class TrackObject
    {
        long id;
        boolean readable;
        String title;
        String title_short;
        String title_version;
        boolean unseen;
        String isrc;
        String link;
        String share;
        int duration;
        int track_position;
        int disc_number;
        int rank;
        String release_date;
        boolean explicit_lyrics;
        int explicit_content_lyrics;
        int explicit_content_cover;
        String preview;
        float bpm;
        float gain;
        String[] available_countries;
        TrackObject alternative = null;
        ContributorObject[] contributors;
        String md5_image;
        ArtistObject artist;
        SimpleAlbumObjectWithPictureAndRelease album;
        String type;
    }

    class SimpleAlbumObjectWithPictureAndRelease
    {
        long id;
        String title;
        String cover;
        String cover_small;
        String cover_medium;
        String cover_big;
        String cover_xl;
        String release_date;
        String type;
    }

    class SimpleArtistObjectWithPictureAndTracklist
    {
        long id;
        String name;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
        String tracklist;
        String md5_image;
        String type;
    }

    class UserAlbumObject
    {
        long id;
        String title;
        String link;
        String cover;
        String cover_small;
        String cover_medium;
        String cover_big;
        String cover_xl;
        int nb_tracks;
        String release_date;
        String record_type;
        boolean available;
        // Alternative;...
        boolean explicit_lyrics;
        int time_add; // type TIMESTAMP ?
        SimpleArtistObjectWithPictureAndTracklist artist;
        String type;
    }

    class UserAlbumsObject
    {
        UserAlbumObject[] data;
        String checksum;
        int total;
        String next = null;
    }

    class SimpleAlbumObjectWithPictureAndTracklist
    {
        long id;
        String title;
        String cover;
        String cover_small;
        String cover_medium;
        String cover_big;
        String cover_xl;
        String md5_image;
        String tracklist;
        String type;
    }

    class UserTrackObject
    {
        long id;
        boolean readable;
        String title;
        String link;
        int duration;
        int rank;
        boolean explicit_lyrics;
        int explicit_content_lyrics;
        int explicit_content_cover;
        String md5_image;
        String md5_origin;
        int media_version;
        String filesize_128;
        String filesize_320;
        String filesize_misc;
        int lyrics_id;
        int time_add;
        SimpleAlbumObjectWithPictureAndTracklist album;
        SimpleArtistObjectWithPictureAndTracklist artist;
    }

    class UserTracksObject
    {
        UserTrackObject[] data;
        int total;
        String next = null;
    }

    class UserPlaylistObject
    {
        long id;
        String title;
        int duration;
        @SerializedName("public")
        boolean isPublic;
        boolean is_loved_track;
        boolean collaborative;
        int nb_tracks;
        int fans;
        String link;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
        String checksum;
        String tracklist;
        String creation_date;
        String md5_image;
        String picture_type;
        int time_add;
        int time_mod;
        PlaylistCreatorObject creator;
        String type;
    }

    class PlaylistCreatorObject
    {
        long id;
        String name;
        String tracklist;
        String type;
    }

    class UserPlaylistsObject
    {
        UserPlaylistObject[] data;
        int total;
        String next = null;
    }

    class PlaylistObject
    {
        long id;
        String title;
        String description;
        int duration;
        @SerializedName("public")
        boolean isPublic;
        boolean is_loved_track;
        boolean collaborative;
        int nb_tracks;
        int fans;
        String link;
        String share;
        String picture;
        String picture_small;
        String picture_medium;
        String picture_big;
        String picture_xl;
        String checksum;
        String tracklist;
        String creation_date;
        String md5_image;
        String picture_type;
        PlaylistCreatorObject creator;
        String type;
        PlaylistTracksObject tracks;
    }

    class PlaylistTracksObject
    {
        UserTrackObject[] data;
        String checksum;
    }

    @GET("user/me/albums")
    Call<UserAlbumsObject> getUserAlbums(@Query("access_token") String token, @Query("limit") int limit, @Query("index") int index);

    @GET("album/{id}")
    Call<AlbumObject> getAlbum(@Path("id") long id);

    @GET("user/me/tracks")
    Call<UserTracksObject> getUserTracks(@Query("access_token") String token, @Query("limit") int limit, @Query("index") int index);

    @GET("user/me/playlists")
    Call<UserPlaylistsObject> getUserPlaylists(@Query("access_token") String token, @Query("limit") int limit, @Query("index") int index);

    @GET("playlist/{id}")
    Call<PlaylistObject> getPlaylist(@Path("id") long id, @Query("access_token") String token);
}
