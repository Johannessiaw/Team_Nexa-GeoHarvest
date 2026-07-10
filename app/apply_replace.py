import sys

filepath = "src/main/java/com/example/ui/screens/GeoHarvestScreens.kt"
new_code_path = "marketplace_new.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

with open(new_code_path, "r", encoding="utf-8") as f:
    new_code = f.read()

start_marker = "fun MarketplaceScreen(viewModel: GeoHarvestViewModel) {"
end_marker = "// ---------------------- 3. SMART LOGISTICS & LIVE TIMELINE ----------------------"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx == -1:
    print("Error: start marker not found")
    sys.exit(1)

if end_idx == -1:
    print("Error: end marker not found")
    sys.exit(1)

final_new_code = new_code.strip() + "\n\n"

replaced_content = content[:start_idx] + final_new_code + content[end_idx:]

with open(filepath, "w", encoding="utf-8") as f:
    f.write(replaced_content)

print("Replacement applied successfully!")
