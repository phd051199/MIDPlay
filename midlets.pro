-injars  dist/MIDPlay_midlet.jar
-outjars dist/MIDPlay.jar

-libraryjars C:/Java_ME_platform_SDK_3.4/lib/cldc_1.1.jar
-libraryjars C:/Java_ME_platform_SDK_3.4/lib/midp_2.0.jar

-microedition

-overloadaggressively
-repackageclasses ''

-allowaccessmodification
-printmapping out.map

-keep public class app.MIDPlay extends javax.microedition.midlet.MIDlet {
    public <init>();
    public void startApp();
    public void pauseApp();
    public void destroyApp(boolean);
}

-dontwarn java.lang.**
-dontwarn javax.microedition.**
-dontwarn org.json.**

-dontoptimize
-flattenpackagehierarchy
