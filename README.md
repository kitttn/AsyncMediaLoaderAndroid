# Media loader fragment

This simple yet powerful fragment allows you to convert selected or captured photos to Java `File` object to send it later to your server, or to get `Bitmap` for showing them in your `<ImageView>`s.

## Installation

Step 1. Add the JitPack repository to your build file

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Step 2. Add the dependency

```
dependencies {
	compile 'com.github.kitttn:async-media-loader-android:0.9.2'
}
```

## Just show me how it works. 

Sure. Just import it in your Activity / Fragment (via `childFragmentManager`) etc. :

```
private val mediaFragment = MediaChooserFragment.newInstance()
```

Also don't forget to attach it: 

```
supportFragmentManager.beginTransaction()
    .add(mediaFragment, "ANY TAG YOU WISH")
    .commit()
```

Then all you need is to call `openCamera()` or `openGallery()` on `mediaFragment` instance: 

```
cameraPhotoBtn.setOnClickListener { mediaFragment.openCamera() }
```

**That's it!** 
This fragment will automatically handle permissions request and photo processing. All data processed will be delivered via `Handler`s you defined while creating instance of MediaChooserFragment: 

```
val mediaFragment = MediaChooserFragment.newInstance(fileHandler = { /* for File objects */ }, bitmapHandler = { /* for BitmapObjects */ }, errorHandler = { /*for all errors */ })
```

This fragment is written in Kotlin and uses coroutines to make it hard job off the main thread. 

Pull requests are welcome!

## Known bugs:

1. A bit difficult to use it from Java. 

 Yes. `newInstance()` definitely needs `@JvmOverloads` annotation for ability to add only required methods from Java. 

 Solution: use Kotlin or Submit PR :)

2. Handling permissions requests is limited and responses are not supported. 

 Yes, you are right. The main idea was to keep this library as a quick option to request files.

 Solution: Submit PR :)


**Thanks for looking. Give it a try :)**

