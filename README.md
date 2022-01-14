# Blade Player
![GitHub release (latest by date)](https://img.shields.io/github/v/release/vhaudiquet/BladePlayer?style=flat-square)
![license](https://img.shields.io/github/license/vhaudiquet/BladePlayer?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/vhaudiquet/BladePlayer?style=flat-square)
![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/vhaudiquet/BladePlayer/latest?style=flat-square)

<!-- Logo -->

Blade is an open source music player for Android, allowing you to play music from multiple
services : files on your phone, [Spotify], and more.

Blade is available on [Google Play], or [here on GitHub].

<div align="center" style="text-align: center;">
  <img src="https://valou3433.fr/blade0.png" width="350" style="max-width: 350;"/>
  <img src="https://valou3433.fr/blade1.png" width="350" style="max-width: 350;"/>
</div>

## Feature overview

### About Spotify

You will need a **Spotify Premium** account to play music from [Spotify], but you can use Blade
without a premium account (to play your Spotify playlists from other sources, for example)

Blade is using the official [Spotify Android Auth] library and [Retrofit] to access
the [Spotify Web API], i.e. to obtain user library and playlists. In order to play music from
Spotify, Blade uses the [librespot-java] library.

When connecting Blade and Spotify, i am for now obligated to ask directly for your username and
password, 2 times : one for [librespot-java], which requires them directly (Spotify does not allow
streaming music from their API authentification), and one for [Spotify Android Auth], which uses the
secure OAuth2 protocol. I can only promise you that i am not stealing your credentials ; if you are
paranoid, you may build Blade from sources or use a network analyzer to see that all network traffic
goes to [Spotify] servers.

Special thanks to the people at [librespot-org] and [librespot-java] ; without them, Spotify support
would not have been possible.

## Contributing

## Older versions

If you want older (i.e. < 2.0) versions of Blade, you can check the [old repository].

[Google Play]:https://play.google.com/store/apps/details?id=v.blade

[here on GitHub]:https://github.com/vhaudiquet/BladePlayer/releases

[Spotify]:https://www.spotify.com

[old repository]:https://github.com/vhaudiquet/blade-player

[Spotify Android Auth]:https://github.com/spotify/android-auth

[Retrofit]:https://github.com/square/retrofit

[Spotify Web API]:https://developer.spotify.com/documentation/web-api/

[librespot-java]:https://github.com/librespot-org/librespot-java

[librespot-org]:https://github.com/librespot-org
