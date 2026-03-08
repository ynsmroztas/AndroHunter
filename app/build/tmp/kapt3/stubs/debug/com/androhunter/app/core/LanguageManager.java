package com.androhunter.app.core;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\t\u001a\u00020\nJ\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eJ\u0006\u0010\u000f\u001a\u00020\u0010J\u0006\u0010\u0011\u001a\u00020\u0010J\u0006\u0010\u0012\u001a\u00020\fJ\u000e\u0010\u0013\u001a\u00020\f2\u0006\u0010\u0014\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/androhunter/app/core/LanguageManager;", "", "()V", "KEY_FIRST", "", "KEY_LANG", "PREF_NAME", "prefs", "Landroid/content/SharedPreferences;", "getLanguage", "Lcom/androhunter/app/core/AppLanguage;", "init", "", "context", "Landroid/content/Context;", "isFirstLaunch", "", "isTurkish", "setFirstLaunchDone", "setLanguage", "lang", "app_debug"})
public final class LanguageManager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_NAME = "androhunter_prefs";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_LANG = "selected_language";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FIRST = "is_first_launch";
    private static android.content.SharedPreferences prefs;
    @org.jetbrains.annotations.NotNull()
    public static final com.androhunter.app.core.LanguageManager INSTANCE = null;
    
    private LanguageManager() {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final boolean isFirstLaunch() {
        return false;
    }
    
    public final void setFirstLaunchDone() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.androhunter.app.core.AppLanguage getLanguage() {
        return null;
    }
    
    public final void setLanguage(@org.jetbrains.annotations.NotNull()
    com.androhunter.app.core.AppLanguage lang) {
    }
    
    public final boolean isTurkish() {
        return false;
    }
}