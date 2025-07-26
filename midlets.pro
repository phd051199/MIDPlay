-injars  dist/MIDPlay_midlet.jar
-outjars dist/MIDPlay.jar

-libraryjars lib/cldc_1.1.jar
-libraryjars lib/midp_2.0.jar

-microedition

-overloadaggressively
-repackageclasses ''

-allowaccessmodification
-printmapping out.map

-keep public class MIDPlay extends javax.microedition.midlet.MIDlet {
    public void startApp();
    public void pauseApp();
    public void destroyApp(boolean);
}

-dontwarn java.lang.**
-dontwarn javax.microedition.**
-dontwarn org.json.**
-dontnote java.io.ByteArrayOutputStream

-dontoptimize
-flattenpackagehierarchy
