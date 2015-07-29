To run the espresso tests:

mm

adb install -r $OUT/data/app/CMFileManagerAndroidTests/CMFileManagerAndroidTests.apk

adb shell am instrument -w -e class com.cyanogenmod.filemanager.activities.WelcomeActivityTest com.cyanogenmod.filemanager.test/android.support.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class com.cyanogenmod.filemanager.activities.MainActivityTest com.cyanogenmod.filemanager.test/android.support.test.runner.AndroidJUnitRunner
