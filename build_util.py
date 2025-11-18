import re
import os

CPP_DIR = os.path.join(os.getcwd(), "src")
OUTPUT_FILE = os.path.join(CPP_DIR, "cbe.cpp")

CPP_KEYWORDS = {"const", "volatile", "static", "mutable", "register", "inline", "signed", "unsigned", "short", "long"}

cbe_pattern = re.compile(r'\b(cbe\s+(?:class|struct))\s+(\w+)\s*\{', re.MULTILINE)
prop_pattern = re.compile(
    r'\bprop\s*\((.*?)\)\s*([A-Za-z_:\d<>\s,\*\&]+)\s+(\w+)\s*;',
    re.MULTILINE
)
field_pattern = re.compile(r'\b([\w:<>\s]+)\s+(\w+)\s*;', re.MULTILINE)

def clean_type(type_str):
    parts = type_str.split()
    clean_parts = [p for p in parts if p not in CPP_KEYWORDS]
    return " ".join(clean_parts)

def split_args(arg_str):
    args = []
    current = ""
    depth = 0
    for c in arg_str:
        if c in "<({[":
            depth += 1
        elif c in ">)}]":
            depth -= 1
        if c == "," and depth == 0:
            args.append(current.strip())
            current = ""
        else:
            current += c
    if current.strip():
        args.append(current.strip())
    return args


def parse_cpp_file(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    classes = []
    for match in cbe_pattern.finditer(content):
        name = match.group(2)
        start = match.end()

        # Find the body
        brace_count = 1
        end = start
        while brace_count > 0 and end < len(content):
            if content[end] == '{':
                brace_count += 1
            elif content[end] == '}':
                brace_count -= 1
            end += 1
        body = content[start:end-1]

        props = []

        # Parse prop fields
        for prop_match in prop_pattern.finditer(body):
            macro_args = prop_match.group(1).strip()
            type_str = clean_type(prop_match.group(2).strip())
            field_name = prop_match.group(3).strip()
            props.append((field_name, type_str, split_args(macro_args)))

        prop_names = {p[0] for p in props}

        # Parse other fields
        for field_match in field_pattern.finditer(body):
            field_name = field_match.group(2).strip()
            if field_name in prop_names:
                continue
            type_str = clean_type(field_match.group(1).strip())
            props.append((field_name, type_str, []))

        classes.append((name, props))

    return classes

def split_template_args(arg_str):
    """
    Split template arguments into a list while respecting nested <>
    Example: "A<B<C>, D>, E" -> ["A<B<C>, D>", "E"]
    """
    args = []
    current = ""
    depth = 0

    for ch in arg_str:
        if ch == '<':
            depth += 1
            current += ch
        elif ch == '>':
            depth -= 1
            current += ch
        elif ch == ',' and depth == 0:
            # argument boundary
            args.append(current.strip())
            current = ""
        else:
            current += ch

    if current.strip():
        args.append(current.strip())

    return args

def split_template_type(type_str):
    type_str = type_str.strip()

    if '<' not in type_str:
        return type_str, None

    base_end = type_str.index('<')
    base = type_str[:base_end].strip()

    # extract Args
    depth = 0
    args = ""
    for ch in type_str[base_end+1:]:
        if ch == '<':
            depth += 1
        elif ch == '>':
            if depth == 0:
                break
            depth -= 1
        args += ch

    return base, split_template_args(args.strip())

def parse_type(type_str):
    """
    Detect if type_str is a template. 
    Returns (base_type, template_args) or (type_str, None)
    """
    type_str = type_str.strip()
    if '<' in type_str and '>' in type_str:
        return split_template_type(type_str)
    else:
        return type_str, None

def encode_arg(data, is_arg):
    return f"    data += {data};\n" if is_arg else f"    {data};\n"

def decode_arg(data, is_arg):
    return f"    {data};\n"
    
def encode_vector(field_name, template_arg, macro_args):
    code = f"    data += EncodeVarInt(static_cast<uint64_t>(obj.{field_name}.size()));\n"
    code += f"    for (const auto& item : obj.{field_name}) {'{'}\n    {encode_field('item', template_arg, macro_args, is_child='')}{'    }'}\n"
    return code

std_variant = {
    'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        uint64_t index = static_cast<uint64_t>({is_child}{field_name}.index());
        data += EncodeVarInt(index);
        std::visit([&data](auto&& arg) {{
            data += encode(arg);
        }}, {is_child}{field_name});
    }}""",
    'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: (
        "    {\n"
        "        uint64_t index = DecodeVarInt(&stream);\n"
        "        switch (index) {\n" +
        ''.join([f'            case {i}: {{ {is_child}{field_name} = Decoder<{arg}>::decode(stream); break; }}\n'
                 for i, arg in enumerate(template_args)]) +
        "            default: throw std::runtime_error(\"Invalid variant index\");\n"
        "        }\n"
        "    }\n"
    ),
}

encoders = {
    'uint64_t': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += EncodeVarInt({is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = DecodeVarInt(&stream);",
    },
    'uuid': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += EncodeVarInt({is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = DecodeVarInt(&stream);",
    },
    'std::string': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += EncodeVarBytes({is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = DecodeVarBytes(&stream);",
    },
    'bool': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += bytes(1, {is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = stream.ReadByte() == 0 ? false : true;",
    },
    'byte': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += bytes(1, {is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = stream.ReadByte();",
    },
    'bytes': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += EncodeVarBytes({is_child}{field_name});",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = DecodeVarBytes(&stream);",
    },
    'const_bytes': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += {is_child}{field_name};",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    {is_child}{field_name} = stream.ReadBytes({template_args[0]});",
    },
    'bls::G1Element': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += {is_child}{field_name}.Serialize();",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        bytes g1_bytes = stream.ReadBytes(bls::G1Element::SIZE);
        {is_child}{field_name} = bls::G1Element::FromBytes(g1_bytes.to_vector());
    }}""",
    },
    'bls::G2Element': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += {is_child}{field_name}.Serialize();",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        bytes g2_bytes = stream.ReadBytes(bls::G2Element::SIZE);
        {is_child}{field_name} = bls::G2Element::FromBytes(g2_bytes.to_vector());
    }}""",
    },
    'bls::PrivateKey': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"    data += {is_child}{field_name}.Serialize();",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        bytes sk_bytes = stream.ReadBytes(bls::PrivateKey::SIZE);
        {is_child}{field_name} = bls::PrivateKey::FromBytes(sk_bytes.to_vector());
    }}""",
    },
    'std::vector': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: encode_vector(field_name, template_args[0], macro_args),
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        uint64_t size = DecodeVarInt(&stream);
        {is_child}{field_name}.clear();
        for (uint64_t i = 0; i < size; ++i) {{
            {is_child}{field_name}.push_back(Decoder<{template_args[0]}>::decode(stream));
        }}
    }}""",
    },
    'std::array': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    for (const auto& item : {is_child}{field_name}) {{
        {encode_field('item', template_args[0], macro_args, is_child='')}
    }}""",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    for (auto& item : {is_child}{field_name}) {{
        item = Decoder<{template_args[0]}>::decode(stream);
    }}""",
    },
    'std::optional': {
        'encode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    if ({is_child}{field_name}.has_value()) {{
        data.push_back(1);
        {encode_field(f"{field_name}.value()", template_args[0], macro_args, is_child=is_child)}
    }} else {{
        data.push_back(0);
    }}""",
        'decode': lambda field_name, base_type, template_args, macro_args, is_child, is_arg: f"""    {{
        byte has_value = stream.ReadByte();
        if (has_value > 0) {{
            {is_child}{field_name} = Decoder<{template_args[0]}>::decode(stream);
        }} else {{
            {is_child}{field_name} = std::nullopt;
        }}
    }}""",
    },
    'std::variant': std_variant,
    'Transaction': std_variant,
    'Commitment': std_variant,
}

def encode_field(field_name, type_str, macro_args, is_child='obj.', is_arg=False):
    if 'ignored' in macro_args:
        return f"    // Ignored vector field: {field_name}\n"
    if type_str is None:
        raise ValueError(f"Type string is None for field '{field_name}'")
    base_type, template_args = parse_type(type_str)
    print(f"Encoding field '{field_name}' of type '{type_str}' (base: '{base_type}', template_args: '{template_args}')")

    enc = ''
    if base_type not in encoders:
        enc = f"    data += encode({is_child}{field_name});"
    else:
        enc = encoders[base_type]['encode'](field_name, base_type, template_args, macro_args, is_child, is_arg)

    return f"{enc}\n"

def decode_field(field_name, type_str, macro_args, is_child='obj.', is_arg=False):
    if 'ignored' in macro_args:
        return f"    // Ignored vector field: {field_name}\n"
    if type_str is None:
        raise ValueError(f"Type string is None for field '{field_name}'")
    base_type, template_args = parse_type(type_str)
    print(f"Decoding field '{field_name}' of type '{type_str}' (base: '{base_type}', template_args: '{template_args}')")

    dec = ''
    if base_type not in encoders:
        dec = f"    {is_child}{field_name} = Decoder<{type_str}>::decode(stream);"
    else:
        dec = encoders[base_type]['decode'](field_name, base_type, template_args, macro_args, is_child, is_arg)

    return f"{dec}\n"

def generate_forward_decls(classes):
    code = "// Forward declarations\n"
    for class_name, _ in classes:
        code += f"template<> bytes encode(const {class_name}& obj);\n"
        code += f"template<> {class_name} decode(SafeStream& stream);\n"
    code += """
template<>
bytes encode(const bytes& obj) {
    return obj;
}

template<>
bytes decode(SafeStream& stream) {
    return DecodeVarBytes(&stream);
}

template<>
uint64_t decode(SafeStream& stream) {
    return DecodeVarInt(&stream);
}
"""
    code += "\n"
    return code

def generate_cbe_implementation(classes, include_headers):
    # Start with includes
    code = '#include "cbe.h"\n'
    code += f'#include <map>\n'
    code += f'#include <set>\n'
    code += f'#include <array>\n'
    code += f'#include <vector>\n'
    code += f'#include <optional>\n'
    for header in include_headers:
        code += f'#include "{header}"\n'
    code += '\n' + generate_forward_decls(classes) + '\n'

    for class_name, fields in classes:
        # Encode
        code += f'template<>\nbytes encode(const {class_name}& obj) {{\n'
        code += '    bytes data;\n'
        for field_name, type_str, macro_args in fields:
            code += encode_field(field_name, type_str, macro_args)
            # code += f'    data += encode_field(data, obj.{field_name});\n'
        code += '    return data;\n'
        code += '}\n\n'

        # Decode<>
        code += f'template<>\n{class_name} decode(SafeStream& stream) {{\n'
        code += f'    {class_name} obj;\n'
        for field_name, type_str, macro_args in fields:
            code += decode_field(field_name, type_str, macro_args)
        code += '    return obj;\n'
        code += '}\n\n'

        # Decode<>
        code += f'template<>\n{class_name} decode(const bytes& data) {{\n'
        code += f'    SafeStream stream;\n'
        code += f'    stream.FromBytes(data);\n'
        code += f'    return decode<{class_name}>(stream);\n'
        code += '}\n\n'

    # Decode<T>
    code += f'template<typename T>\nT decode(const bytes& data) {{\n'
    code += f'    SafeStream stream;\n'
    code += f'    stream.FromBytes(data);\n'
    code += '    return decode<T>(stream);\n'
    code += '}\n\n'

    return code


def main():
    all_classes = []
    include_headers = set(['util.h'])

    for root, _, files in os.walk(CPP_DIR):
        for file in files:
            if file.endswith(".h") or file.endswith(".cpp"):
                path = os.path.join(root, file)
                classes = parse_cpp_file(path)
                if classes:
                    rel_path = os.path.relpath(path, CPP_DIR).replace("\\", "/")
                    include_headers.add(rel_path)  # save relative path for #include
                all_classes.extend(classes)

    impl_code = generate_cbe_implementation(all_classes, include_headers)
    with open(OUTPUT_FILE, 'w') as f:
        f.write(impl_code)

    print(f"Generated {OUTPUT_FILE} with {len(all_classes)} classes/structs and {len(include_headers)} includes.")

if __name__ == "__main__":
    main()
