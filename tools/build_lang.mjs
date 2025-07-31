import fs from "fs/promises";
import path from "path";

const LANG_DIR = "./langs";
const OUTPUT = "./src/Lang.java";

function escapeJava(str) {
  return JSON.stringify(str);
}

function flattenObject(obj, prefix = "") {
  const flattened = {};
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const newKey = prefix ? `${prefix}.${key}` : key;
      if (
        typeof obj[key] === "object" &&
        obj[key] !== null &&
        !Array.isArray(obj[key])
      ) {
        Object.assign(flattened, flattenObject(obj[key], newKey));
      } else {
        flattened[newKey] = obj[key];
      }
    }
  }
  return flattened;
}

// Google Java Format compliant generator
function generateGoogleFormatJava(langs, langCodes, allKeys) {
  const lines = [];

  // Header with proper spacing
  lines.push("import java.util.Hashtable;");
  lines.push("");
  lines.push("public class Lang {");

  // Fields with proper spacing and naming
  lines.push('  private static String currentLang = "en";');
  lines.push("  private static Hashtable langData = new Hashtable();");
  lines.push("  private static boolean initialized = false;");
  lines.push("");

  // Load method with proper formatting
  lines.push("  private static void loadLanguage(String code) {");
  lines.push("    langData.clear();");

  // Generate if-else chain with proper indentation
  let isFirst = true;
  for (const code of langCodes) {
    const condition = isFirst ? "if" : "} else if";
    lines.push(`    ${condition} ("${code}".equals(code)) {`);
    isFirst = false;

    // Add translations with proper spacing
    const map = langs[code];
    for (const key of allKeys) {
      const value = (map[key] ?? "").trim();
      const fallback = langs.en[key] ?? key;
      const finalVal = value.length > 0 ? value : fallback;
      lines.push(`      langData.put("${key}", ${escapeJava(finalVal)});`);
    }
  }

  // Default case
  lines.push("    } else {");
  for (const key of allKeys) {
    const fallback = langs.en[key] ?? key;
    lines.push(`      langData.put("${key}", ${escapeJava(fallback)});`);
  }
  lines.push("    }");
  lines.push("  }");
  lines.push("");

  // Public methods with proper formatting
  lines.push("  public static void setLang(String code) {");
  lines.push("    if (!code.equals(currentLang)) {");
  lines.push("      currentLang = code;");
  lines.push("      loadLanguage(code);");
  lines.push("      initialized = true;");
  lines.push("    }");
  lines.push("  }");
  lines.push("");

  lines.push("  public static String getCurrentLang() {");
  lines.push("    return currentLang;");
  lines.push("  }");
  lines.push("");

  lines.push("  public static String[] getAvailableLanguages() {");
  const langArray = langCodes.map((code) => `"${code}"`).join(", ");
  lines.push(`    return new String[] {${langArray}};`);
  lines.push("  }");
  lines.push("");

  lines.push("  public static String tr(String key) {");
  lines.push("    if (!initialized) {");
  lines.push("      loadLanguage(currentLang);");
  lines.push("      initialized = true;");
  lines.push("    }");
  lines.push("    String value = (String) langData.get(key);");
  lines.push("    return value != null ? value : key;");
  lines.push("  }");
  lines.push("");

  lines.push("  public static String tr(String key, String arg) {");
  lines.push('    return MIDPlay.replace(tr(key), "{0}", arg);');
  lines.push("  }");
  lines.push("");

  lines.push(
    "  public static String tr(String key, String arg0, String arg1) {"
  );
  lines.push("    String template = tr(key);");
  lines.push('    template = MIDPlay.replace(template, "{0}", arg0);');
  lines.push('    return MIDPlay.replace(template, "{1}", arg1);');
  lines.push("  }");
  lines.push("");

  lines.push("  private Lang() {}");
  lines.push("}");

  return lines.join("\n");
}

// Compact version for J2ME (keeping original optimized version)
function generateCompactJava(langs, langCodes, allKeys) {
  const b = []; // buffer

  // Class header - compact
  b.push("import java.util.Hashtable;");
  b.push("");
  b.push("public class Lang {");
  b.push('  private static String c = "en";'); // current lang
  b.push("  private static Hashtable d = new Hashtable();"); // data
  b.push("  private static boolean i = false;"); // initialized

  // Optimized load method - single method with array lookup
  b.push("");
  b.push("  private static void l(String code) {"); // load
  b.push("    d.clear();");

  // Create switch-like structure but more compact
  let first = true;
  for (const code of langCodes) {
    const prefix = first ? "if" : "} else if";
    b.push(`    ${prefix} ("${code}".equals(code)) {`);
    first = false;

    // Inline key-value pairs for better performance
    const map = langs[code];
    for (const key of allKeys) {
      const value = (map[key] ?? "").trim();
      const fallback = langs.en[key] ?? key;
      const finalVal = value.length > 0 ? value : fallback;
      b.push(`      d.put("${key}", ${escapeJava(finalVal)});`);
    }
  }

  b.push("    } else {");
  // Default fallback to English inline
  for (const key of allKeys) {
    const fallback = langs.en[key] ?? key;
    b.push(`      d.put("${key}", ${escapeJava(fallback)});`);
  }
  b.push("    }");
  b.push("  }");

  // Compact public methods
  b.push("");
  b.push("  public static void setLang(String code) {");
  b.push("    if (!code.equals(c)) {");
  b.push("      c = code;");
  b.push("      l(code);");
  b.push("      i = true;");
  b.push("    }");
  b.push("  }");

  b.push("");
  b.push("  public static String getCurrentLang() { return c; }");

  b.push("");
  b.push("  public static String[] getAvailableLanguages() {");
  const langArray = langCodes.map((code) => `"${code}"`).join(", ");
  b.push(`    return new String[] {${langArray}};`);
  b.push("  }");

  // Most used method - highly optimized
  b.push("");
  b.push("  public static String tr(String k) {");
  b.push("    if (!i) { l(c); i = true; }");
  b.push("    String v = (String) d.get(k);");
  b.push("    return v != null ? v : k;");
  b.push("  }");

  // Template methods - optimized
  b.push("");
  b.push("  public static String tr(String k, String a) {");
  b.push('    return MIDPlay.replace(tr(k), "{0}", a);');
  b.push("  }");

  b.push("");
  b.push("  public static String tr(String k, String a, String b) {");
  b.push("    String t = tr(k);");
  b.push('    t = MIDPlay.replace(t, "{0}", a);');
  b.push('    return MIDPlay.replace(t, "{1}", b);');
  b.push("  }");

  b.push("");
  b.push("  private Lang() {}");
  b.push("}");

  return b.join("\n");
}

// Alternative: Even more optimized version with static arrays
function generateHyperOptimized(langs, langCodes, allKeys) {
  const b = [];

  b.push("import java.util.Hashtable;");
  b.push("");
  b.push("public class Lang {");
  b.push('  private static String c = "en";');
  b.push("  private static Hashtable d;");
  b.push("  private static boolean i = false;");

  // Pre-calculate all translations as static arrays
  b.push("");
  b.push("  private static final String[] KEYS = {");
  const keyChunks = [];
  for (let i = 0; i < allKeys.length; i += 10) {
    const chunk = allKeys
      .slice(i, i + 10)
      .map((k) => `"${k}"`)
      .join(", ");
    keyChunks.push("    " + chunk + (i + 10 < allKeys.length ? "," : ""));
  }
  b.push(keyChunks.join("\n"));
  b.push("  };");

  // Generate compact translation arrays for each language
  for (const code of langCodes) {
    const map = langs[code];
    const varName = code.toUpperCase() + "_VALS";

    b.push("");
    b.push(`  private static final String[] ${varName} = {`);

    const valueChunks = [];
    for (let i = 0; i < allKeys.length; i += 5) {
      const chunk = allKeys
        .slice(i, i + 5)
        .map((key) => {
          const value = (map[key] ?? "").trim();
          const fallback = langs.en[key] ?? key;
          const finalVal = value.length > 0 ? value : fallback;
          return escapeJava(finalVal);
        })
        .join(", ");
      valueChunks.push("    " + chunk + (i + 5 < allKeys.length ? "," : ""));
    }
    b.push(valueChunks.join("\n"));
    b.push("  };");
  }

  // Ultra-compact load method
  b.push("");
  b.push("  private static void l(String code) {");
  b.push("    if (d == null) d = new Hashtable();");
  b.push("    else d.clear();");
  b.push("    String[] vals = EN_VALS;"); // default

  for (const code of langCodes) {
    if (code !== "en") {
      b.push(
        `    if ("${code}".equals(code)) vals = ${code.toUpperCase()}_VALS;`
      );
    }
  }

  b.push("    for (int j = 0; j < KEYS.length; j++) {");
  b.push("      d.put(KEYS[j], vals[j]);");
  b.push("    }");
  b.push("  }");

  // Same public interface
  b.push("");
  b.push("  public static void setLang(String code) {");
  b.push("    if (!code.equals(c)) { c = code; l(code); i = true; }");
  b.push("  }");

  b.push("");
  b.push("  public static String getCurrentLang() { return c; }");

  b.push("");
  b.push("  public static String[] getAvailableLanguages() {");
  const langArray = langCodes.map((code) => `"${code}"`).join(", ");
  b.push(`    return new String[] {${langArray}};`);
  b.push("  }");

  b.push("");
  b.push("  public static String tr(String k) {");
  b.push("    if (!i) { l(c); i = true; }");
  b.push("    String v = (String) d.get(k);");
  b.push("    return v != null ? v : k;");
  b.push("  }");

  b.push("");
  b.push("  public static String tr(String k, String a) {");
  b.push('    return MIDPlay.replace(tr(k), "{0}", a);');
  b.push("  }");

  b.push("");
  b.push("  public static String tr(String k, String a, String b) {");
  b.push("    String t = tr(k);");
  b.push('    return MIDPlay.replace(MIDPlay.replace(t, "{0}", a), "{1}", b);');
  b.push("  }");

  b.push("");
  b.push("  private Lang() {}");
  b.push("}");

  return b.join("\n");
}

async function main() {
  const files = await fs.readdir(LANG_DIR);
  const langs = {};
  const langCodes = [];

  for (const file of files) {
    if (!file.endsWith(".json")) continue;
    const code = path.basename(file, ".json");
    const raw = await fs.readFile(path.join(LANG_DIR, file), "utf-8");
    const jsonData = JSON.parse(raw);
    const flattened = flattenObject(jsonData);
    langs[code] = { lang: code, ...flattened };
    langCodes.push(code);
  }

  if (!langs.en) {
    console.error('❌ Missing "en.json" (fallback base)');
    return;
  }

  const allKeys = Object.keys(langs.en);

  // Choose format style
  const useGoogleFormat = process.argv.includes("--google");
  const useHyperOptimized = process.argv.includes("--hyper");

  let javaCode;
  if (useHyperOptimized) {
    javaCode = generateHyperOptimized(langs, langCodes, allKeys);
  } else if (useGoogleFormat) {
    javaCode = generateGoogleFormatJava(langs, langCodes, allKeys);
  } else {
    javaCode = generateCompactJava(langs, langCodes, allKeys);
  }

  await fs.writeFile(OUTPUT, javaCode, "utf-8");

  const stats = await fs.stat(OUTPUT);
  console.log(`✅ Generated ${OUTPUT}`);
  console.log(`📦 Size: ${(stats.size / 1024).toFixed(1)}KB`);
  console.log(`🌍 Languages: ${langCodes.join(", ")}`);
  console.log(`🔑 Keys: ${allKeys.length}`);
  const mode = useHyperOptimized
    ? "Hyper-optimized"
    : useGoogleFormat
    ? "Google Java Format"
    : "Compact J2ME";
  console.log(`⚡ Mode: ${mode}`);
}

main().catch((err) => {
  console.error("❌ Error:", err);
});
