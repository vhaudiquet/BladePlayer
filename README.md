# Blade Player
![GitHub release (latest by date)](https://img.shields.io/github/v/release/vhaudiquet/BladePlayer?style=flat-square)
![license](https://img.shields.io/github/license/vhaudiquet/BladePlayer?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/vhaudiquet/BladePlayer?style=flat-square)
![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/vhaudiquet/BladePlayer/latest?style=flat-square)

<!-- Logo -->

Blade is an open source music player for Android, allowing you to play music from multiple
services : files on your phone, [Spotify], and more.

Blade is available on [Google Play], or [here on GitHub].
It seems to also be on [IzzySoft F-Droid], and to stay up to date there.

Other repos like ~~[Aptoide]~~ or even ~~[ApkPure]~~, ~~[ApkCombo]~~ are not recommended because
they don't seem
to stay up to date fast, and i can't check if they modified the `apk` before publishing it.

<div align="center" style="text-align: center;">
  <img src="https://valou3433.fr/blade0.png" width="350" style="max-width: 350;"/>
  <img src="https://valou3433.fr/blade1.png" width="350" style="max-width: 350;"/>
</div>

## Feature overview

Blade Player is developed by me ([@vhaudiquet]) alone, so the project cannot be tested on many
devices and scenarios ; if you find an issue, open one here.

- The app will open on your library, categorized as Artists, Albums, Songs, and Playlists (available
  in the navigation drawer).
- It allows you to manage libraries for songs (add/remove from source library) and playlists (
  add/remove from playlist, create/remove playlists)
- It supports Android 'dark theme' (the screenshots above are done on a dark themed system).
- It is completely free (there are no ads, no limited version)
- It caches the library locally, so launching Blade requires virtually no data (only refreshing
  tokens and status of sources servers)
- **(TODO)** It has a 'data saving' mode that allows you to listen to music while consuming very low
  mobile data (by not loading album arts)
- The search feature allows you to search the local library instantly
- The "explore" mode allows you to search and browse sources for new music (for example
  search all Spotify and look for new releases)
- It has a layout that can adapt for tablet users (landscape layout)
- It is available in different languages : English, French, German, Turkish ([and you can easily help me traduce it](CONTRIBUTING.md))

<div align="center" style="text-align: center;">
  <img src="https://valou3433.fr/bladef0.png" width="200" style="max-width: 200;"/>
  <img src="https://valou3433.fr/bladef1.png" width="200" style="max-width: 200;"/>
  <img src="https://valou3433.fr/bladef2.png" width="200" style="max-width: 200;"/>
</div>

<div align="center" style="text-align: center;">
  <img src="https://valou3433.fr/bladelandscape.png" width="600" style="max-width: 600;"/>
</div>

### Supported services (music sources)

- Local (i.e. files on your phone)
- Spotify

### About Spotify

You will need a **Spotify Premium** account to play music from [Spotify], but you can use Blade
without a premium account (to play your Spotify playlists from other sources, for example)

Blade is using the official [Spotify Android Auth] library and [Retrofit] to access
the [Spotify Web API], i.e. to obtain user library and playlists. In order to play music from
Spotify, Blade uses the [librespot-java] library.

For now, Blade will act as a Spotify Connect peripheral. **Please do not try to control Blade with
Spotify Connect**. It won't work, and it will glitch the app.

When connecting Blade and Spotify, i am for now obligated to ask directly for your username and
password, 2 times : one for [librespot-java], which requires them directly (Spotify does not allow
streaming music from their API authentification), and one for [Spotify Android Auth], which uses the
secure OAuth2 protocol. I can only promise you that i am not stealing your credentials ; if you are
paranoid, you may build Blade from sources or use a network analyzer to see that all network traffic
goes to [Spotify] servers.

Special thanks to the people at [librespot-org] and [librespot-java] ; without them, Spotify support
would not have been possible.

## Future updates

New features and bug fixes or improvements are coming. Here is a list of what i want to implement :

- SPOTIFY: Use Android Native Decoders, and/or libtremolo if native decoder not present
- UI: Show albums in an 'album view' instead of a list
- Add new services : SoundCloud, YouTube Music, Amazon Music, Tidal, WebDAV/FTP Servers...
- CORE: 'Blade' playlists, that can contain song from all sources

## Contributing

See [CONTRIBUTING.md]

## Older versions

If you want older (i.e. < 2.0) versions of Blade, you can check the [old repository].

[Google Play]:https://play.google.com/store/apps/details?id=v.blade

[here on GitHub]:https://github.com/vhaudiquet/BladePlayer/releases

[IzzySoft F-Droid]:https://apt.izzysoft.de/fdroid/index/apk/v.blade

[Aptoide]:https://blade-blade.en.aptoide.com/app

[ApkPure]:https://apkpure.com/blade-player/v.blade/

[ApkCombo]:https://apkcombo.com/blade-player/v.blade/

[Spotify]:https://www.spotify.com

[old repository]:https://github.com/vhaudiquet/blade-player

[Spotify Android Auth]:https://github.com/spotify/android-auth

[Retrofit]:https://github.com/square/retrofit

[Spotify Web API]:https://developer.spotify.com/documentation/web-api/

[librespot-java]:https://github.com/librespot-org/librespot-java

[librespot-org]:https://github.com/librespot-org

[@vhaudiquet]:https://github.com/vhaudiquet

[CONTRIBUTING.md]:CONTRIBUTING.md