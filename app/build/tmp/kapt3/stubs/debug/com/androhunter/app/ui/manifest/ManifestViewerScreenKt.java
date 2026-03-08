package com.androhunter.app.ui.manifest;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000:\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000b\u001a\u0016\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u0003\u001a,\u0010\u0005\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\nH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u000b\u0010\f\u001a\"\u0010\r\u001a\u00020\u00012\b\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0011H\u0003\u001a\u001e\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u0014\u001a\u00020\u00072\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u0016H\u0007\u001a\"\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\nH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b\u001a\u0010\u001b\u001a$\u0010\u001c\u001a\u00020\u00012\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00070\u00032\f\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00070\u0003H\u0003\u001a\"\u0010\u001f\u001a\u00020\u00012\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\nH\u0003\u00f8\u0001\u0000\u00a2\u0006\u0004\b \u0010\u001b\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006!"}, d2 = {"ComponentsTab", "", "components", "", "Lcom/androhunter/app/ui/manifest/ManifestItem;", "InfoCard", "label", "", "value", "valueColor", "Landroidx/compose/ui/graphics/Color;", "InfoCard-mxwnekA", "(Ljava/lang/String;Ljava/lang/String;J)V", "InfoTab", "info", "Landroid/content/pm/PackageInfo;", "isDebug", "", "isBackup", "ManifestViewerScreen", "packageName", "onBack", "Lkotlin/Function0;", "PermRow", "perm", "color", "PermRow-4WTKRHQ", "(Ljava/lang/String;J)V", "PermissionsTab", "all", "dangerous", "RiskChip", "RiskChip-4WTKRHQ", "app_debug"})
public final class ManifestViewerScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void ManifestViewerScreen(@org.jetbrains.annotations.NotNull()
    java.lang.String packageName, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ComponentsTab(java.util.List<com.androhunter.app.ui.manifest.ManifestItem> components) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PermissionsTab(java.util.List<java.lang.String> all, java.util.List<java.lang.String> dangerous) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void InfoTab(android.content.pm.PackageInfo info, boolean isDebug, boolean isBackup) {
    }
}