# trackitt ProGuard rules

# Release builds keep nothing in the system log: some messages can include merchant names.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
