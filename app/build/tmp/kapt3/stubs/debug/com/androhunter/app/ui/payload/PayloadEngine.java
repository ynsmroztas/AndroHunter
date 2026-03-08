package com.androhunter.app.ui.payload;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J&\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010\fJ8\u0010\r\u001a\u00020\u00062\u0006\u0010\u000e\u001a\u00020\b2\u0006\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\b\u0010\u0011\u001a\u0004\u0018\u00010\bH\u0086@\u00a2\u0006\u0002\u0010\u0012R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/androhunter/app/ui/payload/PayloadEngine;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "fireHttpPayload", "Lcom/androhunter/app/ui/payload/PayloadResult;", "baseUrl", "", "payload", "type", "Lcom/androhunter/app/ui/payload/PayloadType;", "(Ljava/lang/String;Ljava/lang/String;Lcom/androhunter/app/ui/payload/PayloadType;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fireIntentPayload", "packageName", "className", "extraKey", "dataUri", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class PayloadEngine {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    
    public PayloadEngine(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fireIntentPayload(@org.jetbrains.annotations.NotNull()
    java.lang.String packageName, @org.jetbrains.annotations.NotNull()
    java.lang.String className, @org.jetbrains.annotations.NotNull()
    java.lang.String extraKey, @org.jetbrains.annotations.NotNull()
    java.lang.String payload, @org.jetbrains.annotations.Nullable()
    java.lang.String dataUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.androhunter.app.ui.payload.PayloadResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fireHttpPayload(@org.jetbrains.annotations.NotNull()
    java.lang.String baseUrl, @org.jetbrains.annotations.NotNull()
    java.lang.String payload, @org.jetbrains.annotations.NotNull()
    com.androhunter.app.ui.payload.PayloadType type, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.androhunter.app.ui.payload.PayloadResult> $completion) {
        return null;
    }
}