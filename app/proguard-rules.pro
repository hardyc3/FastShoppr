# FastShoppr release shrinker rules.
# Keep only classes that are reached reflectively or generated at compile time.

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable

# Room: generated implementations and entities are mapped by name.
-keep class com.fastshoppr.app.data.local.FastShopprDatabase { *; }
-keep class com.fastshoppr.app.data.local.entity.** { *; }
-keep interface com.fastshoppr.app.data.local.dao.** { *; }
-keep class **_Impl { *; }

# Hilt/Dagger: generated entry points and injected members.
-keep class **_GeneratedInjector { *; }
-keep class Hilt_** { *; }
-keep class **_HiltModules** { *; }
-keep class **_Factory { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Gson payloads decoded from local JSON.
-keep class com.fastshoppr.app.domain.model.NearbyStore { *; }
-keep class com.fastshoppr.app.domain.model.StoreBrand { *; }
-keep class com.fastshoppr.app.domain.model.NearbyStoreSource { *; }
-keep class com.fastshoppr.app.domain.model.RecognizedLine { *; }
-keep class com.fastshoppr.app.domain.lookup.LookupCandidatesCodec$Candidate { *; }
-keep class com.fastshoppr.app.domain.ai.ItemNameOutputValidator$ModelResponse { *; }
-keep class com.google.gson.reflect.TypeToken { *; }

# ML Kit entry points loaded through reflection by Google Play services.
-keep class com.google.mlkit.genai.prompt.java.** { *; }
-keep class com.google.mlkit.digitalink.** { *; }
-keep class com.google.mlkit.vision.text.** { *; }
