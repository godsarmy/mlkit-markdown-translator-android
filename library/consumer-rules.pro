# Consumer ProGuard rules for this library.

# Flexmark orders parser factories by their runtime class identity. Prevent R8
# from merging distinct dependents, which makes its dependency resolver reject them.
-keep,allowobfuscation class com.vladsch.flexmark.** implements com.vladsch.flexmark.util.dependency.Dependent

# BitFieldSet looks up enum constants by their declared field names.
-keepclassmembers enum com.vladsch.flexmark.** {
    *;
}
