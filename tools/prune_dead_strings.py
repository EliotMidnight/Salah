"""
Remove the string fields nothing reads, from SalahLocalization.kt.

Safe by construction, in two senses:

1. The dead list is computed by *reading* rather than by pattern-matching call
   sites, and it is written to a file first so it can be inspected.

2. The removal refuses to write unless the file's structure is unchanged. Every
   top-level declaration must survive, in the same order, and every class body must
   still contain all of its surviving fields. A bulk delete that drops a closing
   paren - or half of a two-line field declaration - fails here instead of turning
   553 compiler errors into a debugging session.

The earlier attempts failed on exactly (2): a two-line declaration lost its second
line, and the resulting file still *looked* plausible.

Usage: python3 tools/prune_dead_strings.py [--apply]
"""
import os
import re
import sys

P = 'app/src/main/java/com/example/ui/localization/SalahLocalization.kt'
SRC_ROOT = 'app/src'


def strip_strings(line):
    """The line with every string literal removed, so brackets inside text do not count."""
    out = ''
    in_string = False
    escaped = False
    for ch in line:
        if in_string:
            if escaped:
                escaped = False
            elif ch == '\\':
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            continue
        out += ch
    return out


def read(path):
    return open(path, encoding='utf-8').read()


def classes_of(lines):
    """name -> (start, end_exclusive), by counting brackets, never by searching for a name."""
    found = {}
    for k, line in enumerate(lines):
        m = re.match(r'^data class (\w+)\(', line)
        if not m:
            continue
        depth = 0
        opened = False
        for j in range(k, len(lines)):
            for ch in strip_strings(lines[j]):
                if ch in '({[':
                    depth += 1
                    opened = True
                elif ch in ')}]':
                    depth -= 1
            if opened and depth == 0:
                found[m.group(1)] = (k, j + 1)
                break
        else:
            raise AssertionError(f'{m.group(1)} never closes')
    return found


def declared_fields(lines, start, end):
    names = []
    for line in lines[start:end]:
        m = re.match(r'^\s*val (\w+)\s*:', line)
        if m:
            names.append(m.group(1))
    return names


def dead_names(lines, classes):
    """A field is dead when nothing reads it: not in this file's code, and nowhere else."""
    code = []
    for line in lines:
        st = line.strip()
        if (re.match(r'^\s*val \w+\s*:', line)
                or re.match(r'^\s*\w+\s*=\s*"', line)
                or st.startswith(('*', '//', '/*'))):
            continue
        code.append(line)
    code = '\n'.join(code)

    elsewhere = {}
    target = os.path.abspath(P)
    for root, _, files in os.walk(SRC_ROOT):
        for f in files:
            if not f.endswith('.kt'):
                continue
            path = os.path.join(root, f)
            if os.path.abspath(path) == target:
                continue
            for word in re.findall(r'[A-Za-z_][A-Za-z0-9_]*', read(path)):
                elsewhere[word] = elsewhere.get(word, 0) + 1

    dead = []
    for name, (start, end) in classes.items():
        for field in declared_fields(lines, start, end):
            if len(re.findall(r'\b' + re.escape(field) + r'\b', code)) == 0 \
                    and elsewhere.get(field, 0) == 0:
                dead.append(field)
    return dead


def remove_fields(lines, names):
    """Drop the declaration and every per-language assignment, and nothing else.

    **A declaration may span two lines.** When it does, the first line ends in `=`
    and the value is on the next:

        val perVerseDescription: String =
            "Study mode: ...",

    Removing only the first leaves the value behind as a bare string inside a
    parameter list, and the file still *looks* plausible. That is what broke three
    earlier attempts, and it is why the continuation is consumed here rather than
    detected afterwards.
    """
    kill = set()
    for name in names:
        for k, line in enumerate(lines):
            st = line.strip()
            is_decl = re.match(r'^val ' + re.escape(name) + r'\s*:', st)
            is_value = re.match(r'^' + re.escape(name) + r'\s*=\s*"', st)
            if not (is_decl or is_value):
                continue
            kill.add(k)
            if is_decl and st.endswith('='):
                if k + 1 >= len(lines):
                    raise AssertionError(f'{name}: declaration ends in = with no value line')
                kill.add(k + 1)
    kept = [l for k, l in enumerate(lines) if k not in kill]
    return kept, []


def verify(original, rewritten, classes_before, dead):
    """Refuse to write unless the file's shape is unchanged."""
    problems = []

    before_top = [l for l in original if re.match(r'^(data class|object|enum class|@)', l)]
    after_top = [l for l in rewritten if re.match(r'^(data class|object|enum class|@)', l)]
    if before_top != after_top:
        problems.append('a top-level declaration changed or moved')

    after_classes = classes_of(rewritten)
    if set(after_classes) != set(classes_before):
        problems.append(f'the set of classes changed: {set(after_classes)} != {set(classes_before)}')

    for name, (start, end) in after_classes.items():
        survivors = [f for f in declared_fields(rewritten, start, end)]
        expected = [f for f in declared_fields(original, *classes_before[name]) if f not in dead]
        if survivors != expected:
            missing = set(expected) - set(survivors)
            extra = set(survivors) - set(expected)
            problems.append(f'{name}: lost {sorted(missing)}, gained {sorted(extra)}')

    for k, line in enumerate(rewritten):
        if line.strip() == '' and k + 1 < len(rewritten) and rewritten[k + 1].strip() == ')':
            problems.append(f'line {k+2}: a class close immediately follows a blank line')
    return problems


def main():
    apply = '--apply' in sys.argv
    lines = read(P).split('\n')
    classes = classes_of(lines)
    dead = sorted(dead_names(lines, classes))

    print(f'{len(dead)} fields are declared and read by nothing')
    with open('build/dead_strings.txt', 'w', encoding='utf-8') as out:
        out.write('\n'.join(dead) + '\n')

    if not apply:
        print('dry run - pass --apply to write')
        return

    kept, orphans = remove_fields(lines, dead)
    if orphans:
        print('REFUSING to write: a field declaration would lose its value line')
        for line_no, prev in orphans:
            print(f'  line {line_no} orphaned after: {prev}')
        return

    problems = verify(lines, kept, classes, set(dead))
    if problems:
        print('REFUSING to write: structure check failed')
        for p in problems:
            print('  ' + p)
        return

    open(P, 'w', encoding='utf-8').write('\n'.join(kept))
    print(f'removed {len(dead)} fields, {len(lines) - len(kept)} lines')


if __name__ == '__main__':
    main()