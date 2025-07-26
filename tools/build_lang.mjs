import fs from "fs/promises";
import path from "path";

const LANG_DIR = "./langs";
const OUTPUT = "./src/Lang.java";
const CLASS_NAME = "Lang";

const toJavaName = (code) =>
  "lang" + code.charAt(0).toUpperCase() + code.slice(1);

function escapeJava(str) {
  return JSON.stringify(str);
}

async function main() {
  const files = await fs.readdir(LANG_DIR);
  const langs = {};
  const langCodes = [];

  for (const file of files) {
    if (!file.endsWith(".json")) continue;
    const code = path.basename(file, ".json");
    const raw = await fs.readFile(path.join(LANG_DIR, file), "utf-8");
    langs[code] = { lang: code, ...JSON.parse(raw) };
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
  buffers.push(`\npublic class ${CLASS_NAME} {`);
  buffers.push(`    private static String currentLang = "en";`);

  // Generate each lang hashtable
  for (const code of langCodes) {
    buffers.push(
      `    private static final Hashtable ${toJavaName(
        code
      )} = new Hashtable();`
    );
  }

  // langs[] declaration
  buffers.push(`\n    private static final Hashtable[] langs;`);

  buffers.push(`\n    static {`);

  for (const code of langCodes) {
    const map = langs[code];
    const javaName = toJavaName(code);

    buffers.push(`        // Language: ${code}`);
    for (const key of allKeys) {
      const value = (map[key] ?? "").trim();
      const fallback = langs.en[key] ?? key;
      const finalVal = value.length > 0 ? value : fallback;
      buffers.push(
        `        ${javaName}.put(${JSON.stringify(key)}, ${escapeJava(
          finalVal
        )});`
      );
    }
    buffers.push("");
  }

  // Langs array
  const langsArray = langCodes.map((c) => toJavaName(c)).join(", ");
  buffers.push(`        langs = new Hashtable[] { ${langsArray} };`);
  buffers.push(`    }`);

  // setLang
  buffers.push(`\n    public static void setLang(String code) {`);
  buffers.push(`        currentLang = code;`);
  buffers.push(`    }`);

  // getCurrentLang
  buffers.push(`\n    public static String getCurrentLang() {`);
  buffers.push(`        return currentLang;`);
  buffers.push(`    }`);

  // getAvailableLanguages
  buffers.push(`\n    public static String[] getAvailableLanguages() {`);
  buffers.push(`        String[] languages = new String[langs.length];`);
  buffers.push(`        for (int i = 0; i < langs.length; i++) {`);
  buffers.push(`            String langCode = (String) langs[i].get("lang");`);
  buffers.push(
    `            languages[i] = langCode != null ? langCode : "en";`
  );
  buffers.push(`        }`);
  buffers.push(`        return languages;`);
  buffers.push(`    }`);

  // tr()
  buffers.push(`\n    public static String tr(String key) {`);
  buffers.push(`        Hashtable lang = langs[0];`);
  buffers.push(
    `        if ("${langCodes[0]}".equals(currentLang)) {\n` +
      `            lang = ${toJavaName(langCodes[0])};\n` +
      `        }`
  );
  for (let i = 1; i < langCodes.length; i++) {
    const code = langCodes[i];
    buffers.push(
      `        else if ("${code}".equals(currentLang)) {\n` +
        `            lang = ${toJavaName(code)};\n` +
        `        }`
    );
  }

  buffers.push(`        String value = (String) lang.get(key);`);
  buffers.push(`        return value == null ? key : value;`);
  buffers.push(`    }`);

  // tr() with one parameter
  buffers.push(`\n    public static String tr(String key, String arg0) {`);
  buffers.push(`        String template = tr(key);`);
  buffers.push(`        template = MIDPlay.replace(template, "{0}", arg0);`);
  buffers.push(`        return template;`);
  buffers.push(`    }`);

  // tr() with two parameters
  buffers.push(
    `\n    public static String tr(String key, String arg0, String arg1) {`
  );
  buffers.push(`        String template = tr(key);`);
  buffers.push(`        template = MIDPlay.replace(template, "{0}", arg0);`);
  buffers.push(`        template = MIDPlay.replace(template, "{1}", arg1);`);
  buffers.push(`        return template;`);
  buffers.push(`    }`);

  // Private constructor
  buffers.push(`\n    private Lang() {}`);
  buffers.push(`}`);

  await fs.writeFile(OUTPUT, buffers.join("\n"), "utf-8");
  console.log(`✅ Built ${OUTPUT}`);
}

main().catch((err) => {
  console.error("❌ Error:", err);
});
