package v.blade.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import v.blade.R;
import v.blade.library.Song;
import v.blade.ui.PlayActivity;

public class PlayerNotification
{
    private static final int NOTIFICATION_ID = 0x42;
    private static final int REQUEST_CODE = 501;
    private static final String CHANNEL_ID = "v.blade.mediachannel";

    private final MediaBrowserService service;
    private final NotificationManagerCompat notificationManager;
    private Notification notification;
    private final NotificationCompat.Action playAction;
    private final NotificationCompat.Action pauseAction;
    private final NotificationCompat.Action nextAction;
    private final NotificationCompat.Action prevAction;

    private boolean isServiceForeground = false;

    protected PlayerNotification(MediaBrowserService service)
    {
        this.service = service;

        notificationManager = NotificationManagerCompat.from(service);

        playAction = new NotificationCompat.Action(R.drawable.ic_play_notification, service.getString(R.string.play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY));
        pauseAction = new NotificationCompat.Action(R.drawable.ic_pause_notification, service.getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE));
        nextAction = new NotificationCompat.Action(R.drawable.ic_skip_next_notification, service.getString(R.string.skip_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        prevAction = new NotificationCompat.Action(R.drawable.ic_skip_previous_notification, service.getString(R.string.skip_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        //Remove potentially already existing notification
        notificationManager.cancelAll();

        //Create channel for notificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel();
    }

    public void update()
    {
        if(service.playlist == null) return;
        if(service.index >= service.playlist.size()) return;
        Song song = service.playlist.get(service.index);
        if(song == null) return;

        //Update notification
        if(this.notification == null)
        {
            //Update mediaSession metadata
            service.mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.getName())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtistsString())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum().getName())
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.getName())
                    .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, song.getTrackNumber())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, service.current == null ? 0 : service.current.getDuration())
                    .build());

            //There is no notification ; we must display it quick and update it for image later
            Notification notification = getNotification(song, null);
            service.startForeground(NOTIFICATION_ID, notification);
            isServiceForeground = true;
            this.notification = notification;
        }
        else
        {
            //We can wait image loading to update notification
            RequestCreator bigImage = song.getBigImageRequest();
            Target target = new Target()
            {
                void updateEverything(Bitmap bitmap)
                {
                    //Update mediaSession metadata
                    service.mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, song.getName())
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtistsString())
                            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum().getName())
                            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.getName())
                            .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, song.getTrackNumber())
                            .putLong(MediaMetadata.METADATA_KEY_DURATION, service.current == null ? 0 : service.current.getDuration())
                            .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            .build());

                    Notification notification = getNotification(song, bitmap);
                    PlayerNotification.this.notification = notification;
                    notificationManager.notify(PlayerNotification.NOTIFICATION_ID, notification);

                    //This makes our service 'killable' if unbound to activity, as it is
                    // no longer viewed as foreground to the system ;
                    // however it also allows to swipe out the notification
                    if(service.mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
                    {
                        if(!isServiceForeground)
                        {
                            service.startForeground(NOTIFICATION_ID, notification);
                            isServiceForeground = true;
                        }
                    }
                    else
                    {
                        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH);
                        isServiceForeground = false;
                    }
                }

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
                {
                    updateEverything(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable)
                {
                    updateEverything(null);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable)
                {
                }
            };
            if(bigImage != null) bigImage.into(target);
            else target.onBitmapFailed(null, null);
        }
    }

    private void updateForImage(Bitmap image)
    {
        Notification notification = getNotification(service.playlist.get(service.index), image);
        notificationManager.notify(PlayerNotification.NOTIFICATION_ID, notification);
        this.notification = notification;
    }

    private Notification getNotification(Song playing, Bitmap largeIcon)
    {
        return buildNotification(playing, largeIcon).build();
    }

    private NotificationCompat.Builder buildNotification(Song playing, Bitmap largeIcon)
    {
        boolean isPlaying = service.mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID);

        Intent openUI = new Intent(service, PlayActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //Content intent : intent on notification click
        int contentIntentFlags = PendingIntent.FLAG_CANCEL_CURRENT;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            contentIntentFlags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentIntent = PendingIntent.getActivity(service, REQUEST_CODE, openUI, contentIntentFlags);

        //NOTE: we actually do not want to kill service when swiping notification i think
        //Intent stopService = new Intent(service, MediaBrowserService.class);
        //stopService.setAction("stop");
        //int deleteIntentFlags = 0;
        //if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        //    deleteIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        //@SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent deleteIntent = null;//PendingIntent.getService(service, REQUEST_CODE, stopService, deleteIntentFlags);

        //MediaStyle, with 3 actions (skip_previous, play/pause, skip_next) and notification building
        androidx.media.app.NotificationCompat.MediaStyle style = new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(service.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(!isPlaying)
                .setCancelButtonIntent(deleteIntent);

        builder.setStyle(style)
                .setWhen(0)
                .setColor(ContextCompat.getColor(service, R.color.bladeGrey)) //TODO change to theme colorPrimary ?
                .setSmallIcon(R.drawable.ic_blade_notification)
                .setContentIntent(contentIntent)
                .setContentTitle(playing.getName())
                .setContentText(playing.getArtistsString() + " - " + playing.getAlbum().getName())
                .setDeleteIntent(deleteIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying);

        builder.addAction(prevAction);
        builder.addAction(isPlaying ? pauseAction : playAction);
        builder.addAction(nextAction);

        //TODO : actually we should replace that with remoteview and stuff like here i think :
        // https://stackoverflow.com/questions/26888247/easiest-way-to-use-picasso-in-notification-icon
        //TODO : there could be a weird race condition here, if you skip before image loading, and
        // then notification is updated, you get notification for older song ; adding a volatile variable
        // to control the notification change should fix that
        //Set large icon if not null, on image load
        if(largeIcon != null) builder.setLargeIcon(largeIcon);
        else if(playing.getBigImageRequest() != null)
            playing.getBigImageRequest().into(new Target()
            {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from)
                {
                    ContextCompat.getMainExecutor(service).execute(() ->
                    {
                        //Update mediaSession metadata
                        service.mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                                .putString(MediaMetadata.METADATA_KEY_TITLE, playing.getName())
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, playing.getArtistsString())
                                .putString(MediaMetadata.METADATA_KEY_ALBUM, playing.getAlbum().getName())
                                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, playing.getName())
                                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, playing.getTrackNumber())
                                .putLong(MediaMetadata.METADATA_KEY_DURATION, service.current == null ? 0 : service.current.getDuration())
                                .putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                .build());

                        updateForImage(bitmap);
                    });
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable)
                {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable)
                {
                }
            });
        else
            builder.setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.drawable.ic_album));

        return builder;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel()
    {
        NotificationManager mNotificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, service.getString(R.string.notification_channel_name), importance);

        channel.setDescription(service.getString(R.string.notification_channel_description));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(channel);
    }
}
