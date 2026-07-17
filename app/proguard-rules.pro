# Persisted enum names are part of the Room and JSON configuration formats.
# Keep their names stable across optimized release builds and app updates.
-keepnames enum de.thorstream.butler.domain.model.**
-keepclassmembers enum de.thorstream.butler.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JSONObject calls the model conversion code directly; no broad reflection
# rules are required. Hilt and Room contribute their own consumer rules.
