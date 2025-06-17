-injars  C:/Mac/Home/Documents/NetBeansProjects/MIDPlay/dist/MIDPlay.jar
-outjars C:/Mac/Home/Documents/NetBeansProjects/MIDPlay/dist/MIDPlay_midlet.jar

-libraryjars C:/Java_ME_platform_SDK_3.4/lib/cldc_1.1.jar
-libraryjars C:/Java_ME_platform_SDK_3.4/lib/midp_2.0.jar

-microedition

-overloadaggressively
-overloadaggressively
-repackageclasses ''

-allowaccessmodification
-printmapping out.map

-keep public class app.MIDPlay extends javax.microedition.midlet.MIDlet {
    public <init>();           # Keep the constructor
    public void startApp();    # Keep the startApp method
    public void pauseApp();    # Keep the pauseApp method
    public void destroyApp(boolean); # Keep the destroyApp method
}

-dontwarn java.lang.**
-dontwarn javax.microedition.**
-dontwarn org.json.**

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable
