# Keep API entities
-keep class com.teo.ttasks.api.entities.** { *; }

# Google API
-dontwarn com.google.api.client.**
# Needed to keep generic types and @Key annotations accessed via reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclasseswithmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keepclasseswithmembers class * {
  @com.google.api.client.util.Value <fields>;
}
-keepnames class com.google.api.client.http.HttpTransport
# Needed by google-http-client-android when linking against an older platform version
-dontwarn com.google.api.client.extensions.android.**
# Needed by google-api-client-android when linking against an older platform version
-dontwarn com.google.api.client.googleapis.extensions.android.**

# Okio
-dontwarn okio.**

# Duplicate class definitions
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

# Play Services
-dontwarn com.google.common.cache.**
-dontwarn com.google.common.primitives.UnsignedBytes$**

# Retrofit 2.X
## https://square.github.io/retrofit/ ##

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Picasso
-dontwarn com.squareup.okhttp.**

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Retrolambda
-dontwarn java.lang.invoke.*
