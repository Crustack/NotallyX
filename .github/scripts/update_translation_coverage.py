import os
import xml.etree.ElementTree as ET
from pathlib import Path
import re

BASE_PATH = Path("shared/src/androidMain/res")
BASE_DIR = BASE_PATH / "values"
OUTPUT_FILE = Path("TRANSLATIONS.md")
LOCALE_FOLDER_PATTERN = re.compile(r"^values-([a-z]{2,3}(-r[A-Z]{2})?)$")
LOCALE_FLAGS = {
    "en": ("🇺🇸", "English"),
    "ca": ("🇪🇸", "Catalan"),
    "cs": ("🇨🇿", "Czech"),
    "da": ("🇩🇰", "Danish"),
    "de": ("🇩🇪", "German"),
    "el": ("🇬🇷", "Greek"),
    "es": ("🇪🇸", "Spanish"),
    "fr": ("🇫🇷", "French"),
    "hu": ("🇭🇺", "Hungarian"),
    "id": ("🇮🇩", "Indonesian"),
    "it": ("🇮🇹", "Italian"),
    "ja": ("🇯🇵", "Japanese"),
    "my": ("🇲🇲", "Burmese"),
    "nb": ("🇳🇴", "Norwegian Bokmål"),
    "nl": ("🇳🇱", "Dutch"),
    "nn": ("🇳🇴", "Norwegian Nynorsk"),
    "pl": ("🇵🇱", "Polish"),
    "pt-rbr": ("🇧🇷", "Portuguese (Brazil)"),
    "pt-rpt": ("🇵🇹", "Portuguese (Portugal)"),
    "ro": ("🇷🇴", "Romanian"),
    "ru": ("🇷🇺", "Russian"),
    "sk": ("🇸🇰", "Slovak"),
    "sl": ("🇸🇮", "Slovenian"),
    "sv": ("🇸🇪", "Swedish"),
    "tl": ("🇵🇭", "Tagalog"),
    "tr": ("🇹🇷", "Turkish"),
    "uk": ("🇺🇦", "Ukrainian"),
    "vi": ("🇻🇳", "Vietnamese"),
    "zh-rcn": ("🇨🇳", "Chinese (Simplified)"),
    "zh-rtw": ("🇹🇼", "Chinese (Traditional)"),
}


def get_flag_and_label(locale_code: str) -> str:
    code = locale_code.lower()
    flag, label = LOCALE_FLAGS.get(code, ("🏳️", code))  # Fallback flag and code
    return f"{flag} {label}"

def extract_keys(xml_path: Path):
    """Extract unique translation keys from strings.xml including <string>, <plurals>, <string-array>."""
    if not xml_path.exists():
        return set()
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
        keys = set()
        for child in root:
            if child.tag in {"string", "plurals", "string-array"}:
                name = child.attrib.get("name")
                if name:
                    keys.add(name)
        return keys
    except ET.ParseError:
        print(f"Warning: Failed to parse {xml_path}")
        return set()

def generate_coverage_table():
    base_keys = extract_keys(BASE_DIR / "strings.xml")
    total_keys = len(base_keys)
    if total_keys == 0:
        print("No base keys found in values/strings.xml")
        return ""

    coverage_rows = []

    # Include base language explicitly
    coverage_rows.append(("🇺🇸 English", 100, len(base_keys), total_keys))

    # Iterate over locale folders
    for lang_dir in sorted(BASE_PATH.iterdir()):
        if not lang_dir.is_dir():
            continue
        if not LOCALE_FOLDER_PATTERN.match(lang_dir.name):
            continue

        locale_code = lang_dir.name.removeprefix("values-")
        xml_file = lang_dir / "strings.xml"
        keys = extract_keys(xml_file)
        translated_count = len(base_keys.intersection(keys))
        percent = int((translated_count / total_keys) * 100)

        label = get_flag_and_label(locale_code)
        coverage_rows.append((label, percent, translated_count, total_keys))

    # Build markdown table
    table_lines = [
        "| Language | Coverage |",
        "|----------|----------|"
    ]
    for lang, percent, done, total in coverage_rows:
        table_lines.append(f"| {lang} | {percent}% ({done}/{total}) |")

    return "\n".join(table_lines)

def update_readme_section():
    if not OUTPUT_FILE.exists():
        print("TRANSLATIONS.md not found.")
        return

    content = OUTPUT_FILE.read_text(encoding="utf-8")

    start_tag = "<!-- translations:start -->"
    end_tag = "<!-- translations:end -->"

    new_section = start_tag + "\n" + generate_coverage_table() + "\n" + end_tag

    pattern = re.compile(f"{re.escape(start_tag)}.*?{re.escape(end_tag)}", re.DOTALL)

    if pattern.search(content):
        updated_content = pattern.sub(new_section, content)
    else:
        # Append at end if tags not found
        updated_content = content.strip() + "\n\n" + new_section

    OUTPUT_FILE.write_text(updated_content, encoding="utf-8")
    print("TRANSLATIONS.md updated with translation coverage.")

if __name__ == "__main__":
    update_readme_section()
