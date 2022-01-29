# Contributing

Contributions are welcome !
There are many ways you can contribute to Blade : report bugs, suggest features, translate it to
your language, fixing a bug, or implementing a new feature.

## Translating Blade

Like any (i think ?) Android app, Blade uses strings in ressource files : you can find the default (
english) one in [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml). To
translate Blade, just get this file and replace every string in it with their translation ; you can
then send the file to me, and i'll update the language files.

## Contributing code

Blade is entirely programmed in Java for Android, using the AndroidX library, with xml layouts and
ressources. Adding code relying on Kotlin, Jetpack Compose, or other libraries doing 'core' Android
job is obviously bad ; please try to keep the design decisions that were made.

### Code style

Blade has a code style that might be considered unusual for Java. Basically, i write like this :

```
void thisIsFunction(String arg0, int arg1)
{
    if(arg1 == 3) return;
    else if(arg1 == 4)
    {
        System.out.println("4 !");
        doSomething();
    }

    System.out.println(arg1 + ": " + arg0);
}
```

and not like this :

```
void thisIsFunction(String arg0, int arg1) {
    if(arg1 == 3) { 
        return;
    }
    else if(arg1 == 4) {
        System.out.println("4 !");
        doSomething();
    }

    System.out.println(arg1 + ": " + arg0);
}
```

Please try to stick to this style ; if you are using Android Studio or IntelliJ Idea, the project is
configured with this code style, you can just code the way you want and autoformat before commiting.

### Adding new features

If you want to add a new feature to Blade, go ahead. However, know that this is a personal project,
and if i don't like your feature, i won't add it to mainline. It does not mean i hate you or your
code, it is just that i have no interest in that feature, so i don't want it in my Blade version ;
but feel free to keep a fork of the project with your feature.

## Blade internals : global architecture

Blade consists of 4 components :
- UI (User Interface) with activities/settings/...
- Library, handling library and Blade internals Song, Album, Artist, Playlist types
- Source, handling source submodules
- Player, that handles the music playing

## Blade internals : adding a new source

A Source component has to :
- Register songs in library
- Provide a way to play songs of such source in a SourcePlayer
- Have an explore/search adapter
- Provide ways of interacting with it (adding songs to playlists, removing elements, ...)

All of this is done extending the Source class.

## GitHub actions setup

