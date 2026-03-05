package com.protomaps.basemap.names;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.protomaps.basemap.text.TextEngine;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class OsmNames {

  private OsmNames() {}

  private static final String[] ALLOWED_LANGS = new String[]{
    // "ar", // Arabic
    // "cs", // Czech
    // "bg", // Bulgarian
    // "da", // Danish
    // "de", // German
    // "el", // Greek
    "en", // English
    // "es", // Spanish
    // "et", // Estonian
    // "fa", // Persian
    // "fi", // Finnish
    // "fr", // French
    // "ga", // Irish
    // "he", // Hebrew
    // "hi", // Hindi
    // "hr", // Croatian
    // "hu", // Hungarian
    // "id", // Indonesian
    // "it", // Italian
    "ja", // Japanese
    // "ko", // Korean
    // "lt", // Lithuanian
    // "lv", // Latvian
    // "ne", // Nepali
    // "nl", // Dutch
    // "no", // Norwegian
    // "mr", // Marathi
    // "mt", // Maltese
    // "pl", // Polish
    // "pt", // Portuguese
    // "ro", // Romanian
    // "ru", // Russian
    // "sk", // Slovak
    // "sl", // Slovenian
    // "sv", // Swedish
    // "tr", // Turkish
    // "uk", // Ukrainian
    // "ur", // Urdu
    // "vi", // Vietnamese
    // "zh-Hans", // Chinese (Simplified)
    // "zh-Hant" // Chinese (Traditional)
  };

  private static final Set<String> ALLOWED_LANG_SET =
    new HashSet<>(Stream.of(ALLOWED_LANGS).map(s -> "name:" + s).toList());

  public static boolean isAllowed(String osmKey) {
    return ALLOWED_LANG_SET.contains(osmKey);
  }

  public static FeatureCollector.Feature setOsmNames(FeatureCollector.Feature feature, SourceFeature sf,
    int minZoom) {
    for (Map.Entry<String, Object> tag : sf.tags().entrySet()) {
      var key = tag.getKey();
      String value = sf.getTag(key).toString();
      var script = Script.getScript(value);

      if (key.equals("name")) {
        List<String> segments = ScriptSegmenter.segmentByScript(value);
        if (!segments.isEmpty()) {
          int index = 0;
          feature.setAttrWithMinzoom("name", segments.get(index), minZoom);

          script = Script.getScript(segments.get(index));

          if (!script.equals("Latin") && !script.equals("Generic")) {
            feature.setAttrWithMinzoom("script", script, minZoom);
          }

          String encodedValue = TextEngine.encodeRegisteredScripts(segments.get(index));
          if (!encodedValue.equals(segments.get(index))) {
            feature.setAttrWithMinzoom("pgf:name", encodedValue, minZoom);
          }
        }
        if (segments.size() >= 2) {
          int index = 1;
          var name2 = segments.get(index);
          script = Script.getScript(name2);

          // Keep encoded text for scripts we support, but hide Cyrillic name2/script2.
          if (!script.equals("Cyrillic")) {
            feature.setAttrWithMinzoom("name2", name2, minZoom);
            if (!script.equals("Latin") && !script.equals("Generic")) {
              feature.setAttrWithMinzoom("script2", script, minZoom);
            }
          }

          String encodedValue = TextEngine.encodeRegisteredScripts(name2);
          if (!encodedValue.equals(name2)) {
            feature.setAttrWithMinzoom("pgf:name2", encodedValue, minZoom);
          }
        }
      }

      if (isAllowed(key)) {
        feature.setAttrWithMinzoom(key, value, minZoom);
      }

    }

    // Backfill name:zh to name:zh-Hant and name:zh-Hans if those are not available
    // if (sf.hasTag("name:zh")) {
    //   if (!sf.hasTag("name:zh-Hant")) {
    //     feature.setAttrWithMinzoom("name:zh-Hant", sf.getTag("name:zh"), minZoom);
    //   }
    //   if (!sf.hasTag("name:zh-Hans")) {
    //     feature.setAttrWithMinzoom("name:zh-Hans", sf.getTag("name:zh"), minZoom);
    //   }
    // }
    return feature;
  }

  public static FeatureCollector.Feature setOsmRefs(FeatureCollector.Feature feature, SourceFeature sf,
    int minZoom) {
    for (Map.Entry<String, Object> tag : sf.tags().entrySet()) {
      var key = tag.getKey();
      // Short codes (CA not Calif.)
      // (nvkelso 20230801) 58% of state/province nodes have ref values
      if ((key.equals("ref") || key.startsWith("ref:")) && sf.getString(key).length() < 5) {
        ParsePosition pos = new ParsePosition(0);
        NumberFormat.getInstance().parse(sf.getString(key), pos);
        if (sf.getString(key).length() != pos.getIndex()) {
          feature.setAttrWithMinzoom(key, sf.getTag(key), minZoom);
        }
      }
    }
    return feature;
  }
}
