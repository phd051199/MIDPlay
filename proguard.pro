# ====================================================================
# ProGuard Configuration for J2ME MIDlet Applications
# ====================================================================

# --------------------------------------------------------------------
# 1. Input/Output Files
# --------------------------------------------------------------------
# -injars: Your input JAR file (the one containing your compiled MIDlet classes).
#          Typically, this is the JAR produced by your IDE/build system.
# -outjars: The output JAR file after ProGuard has processed it.
#          It's good practice to use a different name to distinguish it.
-injars  C:/Mac/Home/Documents/NetBeansProjects/MIDPlay/dist/MIDPlay.jar
-outjars C:/Mac/Home/Documents/NetBeansProjects/MIDPlay/dist/MIDPlay_obfuscated.jar

# --------------------------------------------------------------------
# 2. Library Jars (J2ME Runtime Libraries)
# --------------------------------------------------------------------
# -libraryjars: These are the JARs that contain the APIs your application uses
#               but whose code you don't want to include or modify in your output JAR.
#               For J2ME, this means the CLDC and MIDP API JARs from your SDK.
#               Replace with the actual paths to your SDK's library JARs.
#               Common paths might be something like:
#               C:/Sun_Java_Wireless_Toolkit-2_5_2_01-windows/lib/cldcapi11.jar
#               C:/Sun_Java_Wireless_Toolkit-2_5_2_01-windows/lib/midpapi20.jar
#               Adjust versions (cldcapi10, midpapi10/11) as per your project's target.
-libraryjars C:/Java_ME_platform_SDK_3.4/lib/cldc_1.1.jar
-libraryjars C:/Java_ME_platform_SDK_3.4/lib/midp_2.1.jar

# --------------------------------------------------------------------
# 3. J2ME Specific Preverification
# --------------------------------------------------------------------
# -microedition: Essential flag for J2ME. It ensures that the output bytecode
#                is preverified, which is a mandatory step for J2ME applications
#                to run on mobile devices.
-microedition

# --------------------------------------------------------------------
# 4. Keep Rules (Crucial for Application Functionality)
#    These rules tell ProGuard what *not* to shrink, optimize, or obfuscate.
# --------------------------------------------------------------------

# 4.1 Keep the main MIDlet class and its lifecycle methods
#     Replace 'com.yourcompany.yourapp.MainMIDlet' with the actual fully
#     qualified name of your main MIDlet class.
-keep public class app.MIDPlay extends javax.microedition.midlet.MIDlet {
    public <init>();           # Keep the constructor
    public void startApp();    # Keep the startApp method
    public void pauseApp();    # Keep the pauseApp method
    public void destroyApp(boolean); # Keep the destroyApp method
}

# 4.2 Keep all native methods (if any - rare in pure J2ME)
-keepclasseswithmembers class * {
    native <methods>;
}

# 4.3 Keep interfaces
-keep interface ** { *; }

# 4.4 Keep custom exceptions or error classes (if caught by type name)
#     Example: -keep public class com.yourcompany.yourapp.MyCustomException extends java.lang.Exception

# 4.5 Keep specific classes/methods/fields if accessed via reflection
#     (e.g., Class.forName("...")). This is highly application-specific.
#     Example: -keep class com.yourcompany.yourapp.SomeClassAccessedViaReflection { *; }

# 4.6 Keep anything that is serialized (if using Java serialization)
-keep class * implements java.io.Serializable { *; }

# 4.7 Keep Enum members (if using Java Enums)
#     (Note: Enums were added in J2ME with CLDC 1.1 and MIDP 2.0.)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --------------------------------------------------------------------
# 5. Obfuscation Settings
# --------------------------------------------------------------------
# -repackageclasses '': Puts all obfuscated classes into the default package (no package).
#                      This is aggressive and often desired for maximum size reduction
#                      and obfuscation in J2ME. You can specify a package like 'proguard'
#                      if you prefer: -repackageclasses 'proguard'
-repackageclasses ''

# -flattenpackagehierarchy 'proguard': Similar to repackageclasses, but for top-level packages.
#                                    Not necessary if using -repackageclasses ''.
# -flattenpackagehierarchy 'proguard'

# -allowaccessmodification: Allows ProGuard to change the access modifiers
#                           (public, private, protected) of classes/members for better obfuscation.
-allowaccessmodification

# -overloadaggressively: Allows methods with the same obfuscated name but different return types.
#                        Can be aggressive and might cause issues on some very old VMs.
# -overloadaggressively

# --------------------------------------------------------------------
# 6. Output Debugging Information (for debugging obfuscated apps)
# --------------------------------------------------------------------
# -printmapping: Generates a mapping file (e.g., 'proguard-mapping.txt') that shows
#                the original names and their obfuscated equivalents.
#                ESSENTIAL for deciphering obfuscated stack traces during debugging.
-printmapping proguard-mapping.txt

# -keepattributes: Tells ProGuard which attributes to keep in the output JAR.
#                  LineNumberTable and SourceFile are useful for debugging.
#                  Signature is important for generic types (if used, rare in older J2ME).
#                  InnerClasses is important for anonymous inner classes.
-keepattributes Signature,SourceFile,LineNumberTable,Exceptions,InnerClasses,Deprecated,EnclosingMethod,Synthetic

# --------------------------------------------------------------------
# 7. Warnings and Optimizations
# --------------------------------------------------------------------
# -dontwarn: Suppresses warnings for missing classes or libraries that ProGuard
#            can't find in your setup but are part of the J2ME runtime on the device.
#            Use this with caution; a warning might indicate a real problem.
-dontwarn java.lang.**
-dontwarn javax.microedition.**
-dontwarn org.w3c.**
-dontwarn org.xml.**
-ignorewarnings

# Add any other specific warnings you want to suppress based on your build output.

# -dontoptimize: Disable all optimization. You generally want optimization,
#                but for initial testing, you might disable it.
# -dontoptimize

# -dontshrink: Disable all shrinking. You generally want shrinking for J2ME.
# -dontshrink

# -optimizations: Allows fine-grained control over specific optimization types.
#                 Some optimizations can be aggressive and might cause issues on
#                 very old J2ME VMs. These are common ones to disable if you hit problems.
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# -verbose: Provides more detailed output during the ProGuard process.
#-verbose