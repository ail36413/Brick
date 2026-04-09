# brick-image consumer ProGuard rules

# Public API — keep all public classes and members
-keep class com.ail.brick.image.BrickImage { *; }
-keep class com.ail.brick.image.BrickImage$ImageConfig { *; }
-keep class com.ail.brick.image.ImageLoadConfig { *; }
-keep class com.ail.brick.image.ImagePreloader { *; }

# Built-in transformations
-keep class com.ail.brick.image.GrayscaleTransformation { *; }
-keep class com.ail.brick.image.ColorFilterTransformation { *; }
-keep class com.ail.brick.image.BorderTransformation { *; }
-keep class com.ail.brick.image.BlurTransformation { *; }
-keep class com.ail.brick.image.ImagePreloader { *; }
