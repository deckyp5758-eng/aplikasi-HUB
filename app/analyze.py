import re
import os

code_path = './app/src/main/assets/apps-script/Code.gs'
html_path = './app/src/main/assets/apps-script/Index.html'
kt_dir = './app/src/main/java'

with open(code_path, 'r', encoding='utf-8') as f:
    code_text = f.read()

with open(html_path, 'r', encoding='utf-8') as f:
    html_text = f.read()

kt_text = ''
for root, dirs, files in os.walk(kt_dir):
    for file in files:
        if file.endswith('.kt'):
            with open(os.path.join(root, file), 'r', encoding='utf-8') as kf:
                kt_text += kf.read() + '\n'

all_text = code_text + '\n' + html_text + '\n' + kt_text

lines = code_text.split('\n')

# 1. Detect functions
funcs = []
for i, line in enumerate(lines, 1):
    m = re.match(r'^\s*function\s+([a-zA-Z0-9_$]+)\s*\(([^)]*)\)', line)
    if m:
        funcs.append({
            'name': m.group(1),
            'params': [p.strip() for p in m.group(2).split(',') if p.strip()],
            'line': i,
            'raw': line.strip()
        })

# Special Google Apps Script entrypoints / trigger functions / web app handlers
gas_entry_points = {
    'doGet', 'doPost', 'onEdit', 'onOpen', 'onChange', 'onInstall',
    'include', 'testSubmit', 'testApi', 'setupTriggers', 'deleteTriggers'
}

unused_funcs = []
for fn in funcs:
    name = fn['name']
    if name in gas_entry_points:
        continue
    pattern = r'\b' + re.escape(name) + r'\b'
    matches = re.findall(pattern, all_text)
    # Check if called or passed as string (e.g., action dispatcher or google.script.run)
    # Matches == 1 means only the function declaration line
    if len(matches) <= 1:
        # Double check if referenced in lower/snake/camel case or inside quotes
        str_matches = re.findall(r'[\'\"`]' + re.escape(name) + r'[\'\"`]', all_text, re.IGNORECASE)
        if not str_matches:
            unused_funcs.append(fn)

# 2. Detect global variables/constants
depth = 0
global_vars = []
for i, line in enumerate(lines, 1):
    stripped = re.sub(r'//.*', '', line)
    # check if at depth 0
    if depth == 0:
        m = re.match(r'^\s*(var|const|let)\s+([a-zA-Z0-9_$]+)', stripped)
        if m:
            global_vars.append({
                'kind': m.group(1),
                'name': m.group(2),
                'line': i,
                'raw': line.strip()
            })
    depth += stripped.count('{') - stripped.count('}')
    if depth < 0:
        depth = 0

unused_globals = []
for g in global_vars:
    name = g['name']
    pattern = r'\b' + re.escape(name) + r'\b'
    matches = re.findall(pattern, code_text)
    if len(matches) <= 1:
        unused_globals.append(g)

# 3. Detect unused local variables inside functions
# Parse function blocks
func_blocks = []
for idx, fn in enumerate(funcs):
    start_line = fn['line']
    end_line = funcs[idx+1]['line'] - 1 if idx + 1 < len(funcs) else len(lines)
    func_lines = lines[start_line-1:end_line]
    func_body = '\n'.join(func_lines)
    
    # find local var declarations: var x = ..., let x = ..., const x = ...
    local_vars = []
    for rel_i, f_line in enumerate(func_lines, 1):
        f_stripped = re.sub(r'//.*', '', f_line)
        m_list = re.findall(r'\b(?:var|let|const)\s+([a-zA-Z0-9_$]+)', f_stripped)
        for var_name in m_list:
            local_vars.append({
                'name': var_name,
                'line': start_line + rel_i - 1,
                'raw': f_line.strip()
            })
    
    for lv in local_vars:
        p = r'\b' + re.escape(lv['name']) + r'\b'
        m = re.findall(p, func_body)
        if len(m) <= 1:
            # unused local var
            # verify it's not a global var shadowed or something
            unused_globals.append({
                'kind': 'local variable',
                'name': lv['name'],
                'line': lv['line'],
                'raw': lv['raw'],
                'func': fn['name']
            })

# Print Results
print("=== UNUSED FUNCTIONS ===")
for uf in unused_funcs:
    print(f"Name: {uf['name']} | Line: {uf['line']} | Code: {uf['raw']}")

print("\n=== UNUSED GLOBALS / VARS ===")
for ug in unused_globals:
    func_info = f" in function {ug['func']}" if 'func' in ug else " (Global)"
    print(f"Name: {ug['name']} | Kind: {ug['kind']}{func_info} | Line: {ug['line']} | Code: {ug['raw']}")
