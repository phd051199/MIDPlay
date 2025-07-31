import fs from "fs/promises";
import path from "path";

const LANG_DIR = "./langs";
const OUTPUT = "./src/Lang.java";
const CLASS_NAME = "Lang";

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

  const buffers = [];
  buffers.push(`import java.util.Hashtable;`);
  buffers.push(`\n`);
  buffers.push(`public class ${CLASS_NAME} {`);
  buffers.push(`  private static String currentLang = "en";`);
  buffers.push(`  private static Hashtable currentLangData = new Hashtable();`);
  buffers.push(`  private static boolean initialized = false;`);
  buffers.push(
    `\n  private static synchronized void loadLanguage(String langCode) {`
  );
  buffers.push(`    currentLangData.clear();`);
  buffers.push(`\n    if ("en".equals(langCode)) {`);
  buffers.push(`      loadEnglish();`);

  // Add conditions for other languages
  for (let i = 1; i < langCodes.length; i++) {
    const code = langCodes[i];
    let methodName;
    if (code === "he") {
      methodName = "loadHebrew";
    } else if (code === "tr") {
      methodName = "loadTurkish";
    } else if (code === "vi") {
      methodName = "loadVietnamese";
    } else {
      methodName = `load${code.charAt(0).toUpperCase() + code.slice(1)}`;
    }
    buffers.push(`    } else if ("${code}".equals(langCode)) {`);
    buffers.push(`      ${methodName}();`);
  }

  buffers.push(`    } else {`);
  buffers.push(`      loadEnglish();`);
  buffers.push(`    }`);
  buffers.push(`  }`);

  // Generate individual load methods for each language
  for (const code of langCodes) {
    const map = langs[code];
    let methodName;
    if (code === "en") {
      methodName = "loadEnglish";
    } else if (code === "he") {
      methodName = "loadHebrew";
    } else if (code === "tr") {
      methodName = "loadTurkish";
    } else if (code === "vi") {
      methodName = "loadVietnamese";
    } else {
      methodName = `load${code.charAt(0).toUpperCase() + code.slice(1)}`;
    }

    buffers.push(`\n  private static void ${methodName}() {`);
    for (const key of allKeys) {
      const value = (map[key] ?? "").trim();
      const fallback = langs.en[key] ?? key;
      const finalVal = value.length > 0 ? value : fallback;
      buffers.push(
        `    currentLangData.put(${JSON.stringify(key)}, ${escapeJava(
          finalVal
        )});`
      );
    }
    buffers.push(`  }`);
  }

  // setLang
  buffers.push(`\n  public static synchronized void setLang(String code) {`);
  buffers.push(`    if (!code.equals(currentLang)) {`);
  buffers.push(`      currentLang = code;`);
  buffers.push(`      loadLanguage(code);`);
  buffers.push(`      initialized = true;`);
  buffers.push(`    }`);
  buffers.push(`  }`);

  // getCurrentLang
  buffers.push(`\n  public static String getCurrentLang() {`);
  buffers.push(`    return currentLang;`);
  buffers.push(`  }`);

  // getAvailableLanguages
  buffers.push(`\n  public static String[] getAvailableLanguages() {`);
  const langCodesArray = langCodes.map((code) => `"${code}"`).join(", ");
  buffers.push(`    return new String[] {${langCodesArray}};`);
  buffers.push(`  }`);

  // tr()
  buffers.push(`\n  public static String tr(String key) {`);
  buffers.push(`    if (!initialized) {`);
  buffers.push(`      loadLanguage(currentLang);`);
  buffers.push(`      initialized = true;`);
  buffers.push(`    }`);
  buffers.push(`    String value = (String) currentLangData.get(key);`);
  buffers.push(`    return value == null ? key : value;`);
  buffers.push(`  }`);

  // tr() with one parameter
  buffers.push(`\n  public static String tr(String key, String arg0) {`);
  buffers.push(`    String template = tr(key);`);
  buffers.push(`    template = MIDPlay.replace(template, "{0}", arg0);`);
  buffers.push(`    return template;`);
  buffers.push(`  }`);

  // tr() with two parameters
  buffers.push(
    `\n  public static String tr(String key, String arg0, String arg1) {`
  );
  buffers.push(`    String template = tr(key);`);
  buffers.push(`    template = MIDPlay.replace(template, "{0}", arg0);`);
  buffers.push(`    template = MIDPlay.replace(template, "{1}", arg1);`);
  buffers.push(`    return template;`);
  buffers.push(`  }`);

  // Private constructor
  buffers.push(`\n  private Lang() {}`);
  buffers.push(`}`);

  await fs.writeFile(OUTPUT, buffers.join("\n"), "utf-8");
  console.log(`✅ Built ${OUTPUT}`);
}

main().catch((err) => {
  console.error("❌ Error:", err);
});
