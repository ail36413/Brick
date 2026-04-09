# brick-ui consumer ProGuard rules

# Custom views referenced from XML need all constructors
-keep class com.ail.brick.ui.statelayout.StateLayout { <init>(...); }
-keep class com.ail.brick.ui.titlebar.TitleBar { <init>(...); }
-keep class com.ail.brick.ui.widget.RoundLayout { <init>(...); }
-keep class com.ail.brick.ui.widget.FlowLayout { <init>(...); }
-keep class com.ail.brick.ui.widget.BadgeView { <init>(...); }

# Keep custom attributes
-keepclassmembers class **.R$styleable { *; }
