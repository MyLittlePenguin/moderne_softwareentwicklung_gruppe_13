#!/usr/bin/env bash
# Lokales Auswertungsskript für Code-, Coverage- und Architekturmetriken
# (Alternative zu SonarQube auf Basis von JaCoCo, PMD/CPD, Checkstyle und Quellcode-Analyse).
#
# Verwendung:
#   ./scripts/check-metrics.sh               # Führt Maven-Läufe + Auswertung aus
#   ./scripts/check-metrics.sh --report-only # Wertet vorhandene Reports in target/ aus

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${PROJECT_ROOT}"

REPORT_ONLY=false
MVN_ARGS=()

for arg in "$@"; do
	case "${arg}" in
		--report-only)
			REPORT_ONLY=true
			;;
		-h|--help)
			echo "Verwendung: $0 [--report-only] [weitere Maven-Argumente]"
			echo "  --report-only  Überspringt den Maven-Lauf und wertet nur target/ aus."
			exit 0
			;;
		*)
			MVN_ARGS+=("${arg}")
			;;
	esac
done

if [[ "${REPORT_ONLY}" == "false" ]]; then
	if [[ -d "${HOME}/.m2/repository" && ! -w "${HOME}/.m2/repository" ]]; then
		MVN_ARGS+=("-Daether.syncContext.named.factory=noop" "-o")
	fi
	echo "==> Starte Tests, JaCoCo-Coverage, PMD/CPD und Checkstyle..."
	./mvnw test jacoco:report pmd:pmd pmd:cpd checkstyle:check "${MVN_ARGS[@]}"
fi

python3 - "${PROJECT_ROOT}" <<'PYEOF'
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

project_root = Path(sys.argv[1])
src_main = project_root / "src" / "main" / "java"
target_dir = project_root / "target"
jacoco_xml = target_dir / "site" / "jacoco" / "jacoco.xml"
pmd_xml = target_dir / "pmd.xml"
cpd_xml = target_dir / "cpd.xml"
checkstyle_xml = target_dir / "checkstyle-result.xml"
report_md_path = target_dir / "metrics-report.md"


def analyze_java_source(path: Path):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    total_lines = len(lines)
    blank_lines = 0
    comment_lines = 0
    code_lines = 0
    in_block_comment = False

    code_only_lines = []

    for line in lines:
        s = line.strip()
        if not s:
            blank_lines += 1
            code_only_lines.append("")
            continue

        has_code = False
        has_comment = False
        i = 0
        n = len(s)
        in_str = False
        in_char = False
        cleaned = []

        while i < n:
            ch = s[i]
            nxt = s[i + 1] if i + 1 < n else ""

            if in_block_comment:
                has_comment = True
                if ch == "*" and nxt == "/":
                    in_block_comment = False
                    i += 2
                else:
                    i += 1
                continue

            if in_str:
                cleaned.append(" ")
                if ch == "\\" and i + 1 < n:
                    i += 2
                elif ch == '"':
                    in_str = False
                    i += 1
                else:
                    i += 1
                continue

            if in_char:
                cleaned.append(" ")
                if ch == "\\" and i + 1 < n:
                    i += 2
                elif ch == "'":
                    in_char = False
                    i += 1
                else:
                    i += 1
                continue

            if ch == "/" and nxt == "/":
                has_comment = True
                break
            if ch == "/" and nxt == "*":
                has_comment = True
                in_block_comment = True
                i += 2
                continue

            if ch == '"':
                in_str = True
                has_code = True
                cleaned.append('"')
                i += 1
                continue
            if ch == "'":
                in_char = True
                has_code = True
                cleaned.append("'")
                i += 1
                continue

            if not ch.isspace():
                has_code = True
            cleaned.append(ch)
            i += 1

        if has_code:
            code_lines += 1
        if has_comment:
            comment_lines += 1
        code_only_lines.append("".join(cleaned))

    cleaned_source = "\n".join(code_only_lines)

    # Methoden und Parameteranzahl im Quellcode bestimmen
    method_Decl_re = re.compile(
        r"(?:public|protected|private|static|final|synchronized|abstract|default|\s)+"
        r"[\w\<\>\[\],\?\s]*?\b([A-Za-z_]\w*)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\{"
    )
    control_keywords = {"if", "for", "while", "switch", "catch", "synchronized", "try"}
    max_params = 0
    param_counts = []
    for match in method_Decl_re.finditer(cleaned_source):
        name = match.group(1)
        if name in control_keywords:
            continue
        params_raw = match.group(2).strip()
        if not params_raw:
            cnt = 0
        else:
            # Generics in Parametern vor dem Split auf Komma neutralisieren
            depth = 0
            cnt = 1
            for c in params_raw:
                if c == "<":
                    depth += 1
                elif c == ">":
                    depth = max(0, depth - 1)
                elif c == "," and depth == 0:
                    cnt += 1
        param_counts.append((name, cnt))
        if cnt > max_params:
            max_params = cnt

    # Record-Header-Komponenten berücksichtigen (für kompakte Record-Konstruktoren)
    record_re = re.compile(r"\brecord\s+([A-Za-z_]\w*)\s*\(([^)]*)\)")
    for rmatch in record_re.finditer(cleaned_source):
        rparams = rmatch.group(2).strip()
        if rparams:
            depth = 0
            rcnt = 1
            for c in rparams:
                if c == "<":
                    depth += 1
                elif c == ">":
                    depth = max(0, depth - 1)
                elif c == "," and depth == 0:
                    rcnt += 1
            if rcnt > max_params:
                max_params = rcnt

    return {
        "file": path.name,
        "rel_path": str(path.relative_to(project_root)),
        "total_lines": total_lines,
        "ncloc": code_lines,
        "comment_lines": comment_lines,
        "blank_lines": blank_lines,
        "max_params": max_params,
        "param_counts": param_counts,
    }


def parse_jacoco(xml_path: Path):
    if not xml_path.exists():
        return None
    tree = ET.parse(xml_path)
    root = tree.getroot()

    def read_counters(elem):
        res = {}
        for c in elem.findall("counter"):
            ctype = c.attrib["type"]
            missed = int(c.attrib["missed"])
            covered = int(c.attrib["covered"])
            total = missed + covered
            pct = (covered / total * 100.0) if total > 0 else 100.0
            res[ctype] = {"missed": missed, "covered": covered, "total": total, "pct": pct}
        return res

    totals = read_counters(root)
    files_data = {}
    classes_data = []

    for pkg in root.findall("package"):
        pkg_name = pkg.attrib["name"].replace("/", ".")
        for cls in pkg.findall("class"):
            cls_name = cls.attrib["name"].split("/")[-1]
            src_fname = cls.attrib.get("sourcefilename", "")
            cls_counters = read_counters(cls)
            classes_data.append({
                "package": pkg_name,
                "class": cls_name,
                "sourcefile": src_fname,
                "counters": cls_counters,
            })

        for sf in pkg.findall("sourcefile"):
            sf_name = sf.attrib["name"]
            sf_counters = read_counters(sf)
            missed_lines = []
            partial_branches = []
            for line in sf.findall("line"):
                nr = int(line.attrib["nr"])
                mi = int(line.attrib.get("mi", "0"))
                ci = int(line.attrib.get("ci", "0"))
                mb = int(line.attrib.get("mb", "0"))
                cb = int(line.attrib.get("cb", "0"))
                if ci == 0 and mi > 0:
                    missed_lines.append(nr)
                if mb > 0:
                    partial_branches.append((nr, cb, mb + cb))

            files_data[sf_name] = {
                "package": pkg_name,
                "counters": sf_counters,
                "missed_lines": missed_lines,
                "partial_branches": partial_branches,
            }

    return {"totals": totals, "files": files_data, "classes": classes_data}


def parse_pmd(xml_path: Path):
    if not xml_path.exists():
        return []
    tree = ET.parse(xml_path)
    root = tree.getroot()
    ns = {"pmd": "http://pmd.sourceforge.net/report/2.0.0"}
    violations = []

    file_elems = root.findall("pmd:file", ns) or root.findall("file")
    for f in file_elems:
        fname = Path(f.attrib["name"]).name
        v_elems = f.findall("pmd:violation", ns) or f.findall("violation")
        for v in v_elems:
            violations.append({
                "file": fname,
                "beginline": int(v.attrib.get("beginline", "0")),
                "endline": int(v.attrib.get("endline", "0")),
                "rule": v.attrib.get("rule", ""),
                "ruleset": v.attrib.get("ruleset", ""),
                "priority": int(v.attrib.get("priority", "3")),
                "message": (v.text or "").strip(),
            })
    return violations


def parse_cpd(xml_path: Path):
    if not xml_path.exists():
        return []
    tree = ET.parse(xml_path)
    root = tree.getroot()
    duplications = []
    for dup in root.findall(".//duplication"):
        lines = int(dup.attrib.get("lines", "0"))
        tokens = int(dup.attrib.get("tokens", "0"))
        files = []
        for f in dup.findall("file"):
            p = Path(f.attrib.get("path", "")).name
            line = int(f.attrib.get("line", "0"))
            endline = int(f.attrib.get("endline", str(line + lines - 1)))
            files.append(f"{p}:{line}-{endline}")
        duplications.append({
            "lines": lines,
            "tokens": tokens,
            "locations": files,
        })
    return duplications


def parse_checkstyle(xml_path: Path):
    if not xml_path.exists():
        return []
    tree = ET.parse(xml_path)
    root = tree.getroot()
    issues = []
    for f in root.findall("file"):
        fname = Path(f.attrib.get("name", "")).name
        for err in f.findall("error"):
            issues.append({
                "file": fname,
                "line": int(err.attrib.get("line", "0")),
                "severity": err.attrib.get("severity", "warning"),
                "message": err.attrib.get("message", ""),
                "source": err.attrib.get("source", "").split(".")[-1],
            })
    return issues


def compress_line_ranges(lines):
    if not lines:
        return "–"
    lines = sorted(set(lines))
    ranges = []
    start = prev = lines[0]
    for nr in lines[1:]:
        if nr == prev + 1:
            prev = nr
        else:
            ranges.append(f"{start}" if start == prev else f"{start}-{prev}")
            start = prev = nr
    ranges.append(f"{start}" if start == prev else f"{start}-{prev}")
    return ", ".join(ranges)


java_files = sorted(src_main.rglob("*.java"))
src_stats = [analyze_java_source(f) for f in java_files]
jacoco_data = parse_jacoco(jacoco_xml)
pmd_issues = parse_pmd(pmd_xml)
cpd_dups = parse_cpd(cpd_xml)
checkstyle_issues = parse_checkstyle(checkstyle_xml)

total_ncloc = sum(s["ncloc"] for s in src_stats)
total_comments = sum(s["comment_lines"] for s in src_stats)
total_lines = sum(s["total_lines"] for s in src_stats)
comment_density = (
    total_comments / (total_ncloc + total_comments) * 100.0
    if (total_ncloc + total_comments) > 0
    else 0.0
)

out_lines = []


def emit(line=""):
    out_lines.append(line)


emit("# Software-, Coverage- und Architekturmetriken (votee_backend)")
emit("")
emit("## 1. Gesamtüberblick (Aufgabe 1)")
emit("")
emit("| Metrik | Wert | Quelle |")
emit("| --- | --- | --- |")
emit(f"| Dateien (`src/main/java`) | {len(src_stats)} | Quellcode |")
emit(f"| Lines of Code (NCLOC) | {total_ncloc} | Quellcode |")
emit(f"| Kommentarzeilen | {total_comments} | Quellcode |")
emit(f"| Kommentaranteil (`Kommentare / (NCLOC + Kommentare)`) | {comment_density:.1f} % | Quellcode |")

if jacoco_data:
    t = jacoco_data["totals"]
    cls_total = t.get("CLASS", {}).get("total", 0)
    m_total = t.get("METHOD", {}).get("total", 0)
    cc_total = t.get("COMPLEXITY", {}).get("total", 0)
    cc_avg = (cc_total / m_total) if m_total > 0 else 0.0
    line_cov = t.get("LINE", {})
    branch_cov = t.get("BRANCH", {})
    instr_cov = t.get("INSTRUCTION", {})
    method_cov = t.get("METHOD", {})

    emit(f"| Klassen (inkl. innerer Typen) | {cls_total} | JaCoCo |")
    emit(f"| Methoden (Bytecode) | {m_total} | JaCoCo |")
    emit(f"| Cyclomatic Complexity (McCabe) | {cc_total} (Ø {cc_avg:.2f} / Methode) | JaCoCo |")
    emit(f"| Line Coverage | {line_cov.get('pct', 0):.1f} % ({line_cov.get('covered', 0)}/{line_cov.get('total', 0)}) | JaCoCo |")
    emit(f"| Branch Coverage | {branch_cov.get('pct', 0):.1f} % ({branch_cov.get('covered', 0)}/{branch_cov.get('total', 0)}) | JaCoCo |")
    emit(f"| Method Coverage | {method_cov.get('pct', 0):.1f} % ({method_cov.get('covered', 0)}/{method_cov.get('total', 0)}) | JaCoCo |")
    emit(f"| Instruction Coverage | {instr_cov.get('pct', 0):.1f} % ({instr_cov.get('covered', 0)}/{instr_cov.get('total', 0)}) | JaCoCo |")

emit(f"| PMD-Regelverletzungen | {len(pmd_issues)} | PMD |")
emit(f"| CPD-Duplikate (≥ 30 Tokens) | {len(cpd_dups)} | PMD CPD |")
emit(f"| Checkstyle-Meldungen | {len(checkstyle_issues)} | Checkstyle |")
emit("")

emit("## 2. Metriken pro Datei (Aufgabe 1)")
emit("")
emit("| Datei | NCLOC | Kommentare (%) | Methoden | Cyclomatic Complexity | Ø CC / Methode | max. Parameter |")
emit("| --- | ---: | ---: | ---: | ---: | ---: | ---: |")

rows_file = []
for s in src_stats:
    fname = s["file"]
    ncloc = s["ncloc"]
    comm = s["comment_lines"]
    cdens = (comm / (ncloc + comm) * 100.0) if (ncloc + comm) > 0 else 0.0
    jfile = jacoco_data["files"].get(fname) if jacoco_data else None
    if jfile:
        m_cnt = jfile["counters"].get("METHOD", {}).get("total", len(s["param_counts"]))
        cc = jfile["counters"].get("COMPLEXITY", {}).get("total", 0)
    else:
        m_cnt = len(s["param_counts"])
        cc = 0
    cc_per_m = (cc / m_cnt) if m_cnt > 0 else 0.0
    rows_file.append((cc, fname, ncloc, cdens, m_cnt, cc_per_m, s["max_params"]))

for cc, fname, ncloc, cdens, m_cnt, cc_per_m, max_p in sorted(rows_file, key=lambda x: (-x[0], x[1])):
    emit(f"| `{fname}` | {ncloc} | {cdens:.1f} % | {m_cnt} | {cc} | {cc_per_m:.2f} | {max_p} |")
emit("")

if jacoco_data:
    emit("## 3. Code Coverage pro Datei (JaCoCo – Aufgabe 2)")
    emit("")
    emit("| Datei | Line Coverage | Branch Coverage | Method Coverage | Nicht abgedeckte Zeilen | Teilweise abgedeckte Branches (Zeile: cov/total) |")
    emit("| --- | ---: | ---: | ---: | --- | --- |")

    for fname, fdata in sorted(jacoco_data["files"].items()):
        c = fdata["counters"]
        lc = c.get("LINE", {"covered": 0, "total": 0, "pct": 100.0})
        bc = c.get("BRANCH", {"covered": 0, "total": 0, "pct": 100.0})
        mc = c.get("METHOD", {"covered": 0, "total": 0, "pct": 100.0})

        lc_str = f"{lc['pct']:.1f} % ({lc['covered']}/{lc['total']})" if lc["total"] > 0 else "–"
        bc_str = f"{bc['pct']:.1f} % ({bc['covered']}/{bc['total']})" if bc["total"] > 0 else "– (keine Branches)"
        mc_str = f"{mc['pct']:.1f} % ({mc['covered']}/{mc['total']})" if mc["total"] > 0 else "–"
        missed_str = compress_line_ranges(fdata["missed_lines"])
        pb_str = (
            ", ".join(f"Z.{nr} ({cb}/{tot})" for nr, cb, tot in fdata["partial_branches"])
            if fdata["partial_branches"]
            else "–"
        )
        emit(f"| `{fname}` | {lc_str} | {bc_str} | {mc_str} | {missed_str} | {pb_str} |")
    emit("")

emit("## 4. Technical Debt, Regelverletzungen & Duplikate (Aufgabe 3)")
emit("")
emit(f"### 4.1 PMD-Meldungen ({len(pmd_issues)})")
emit("")
if pmd_issues:
    emit("| Datei | Zeile | Priorität | Regel | Beschreibung |")
    emit("| --- | ---: | ---: | --- | --- |")
    for v in sorted(pmd_issues, key=lambda x: (x["priority"], x["file"], x["beginline"])):
        line_str = f"{v['beginline']}" if v["beginline"] == v["endline"] else f"{v['beginline']}-{v['endline']}"
        msg = v["message"].replace("|", "\\|").replace("\n", " ")
        emit(f"| `{v['file']}` | {line_str} | P{v['priority']} | `{v['rule']}` | {msg} |")
else:
    emit("_Keine PMD-Regelverletzungen gefunden._")
emit("")

emit(f"### 4.2 Code-Duplikate laut PMD CPD ({len(cpd_dups)})")
emit("")
if cpd_dups:
    emit("| Zeilen | Tokens | Vorkommen |")
    emit("| ---: | ---: | --- |")
    for d in cpd_dups:
        locs = ", ".join(f"`{loc}`" for loc in d["locations"])
        emit(f"| {d['lines']} | {d['tokens']} | {locs} |")
else:
    emit("_Keine Code-Duplikate (≥ 30 Tokens) gefunden._")
emit("")

emit(f"### 4.3 Checkstyle-Meldungen ({len(checkstyle_issues)})")
emit("")
if checkstyle_issues:
    emit("| Datei | Zeile | Schweregrad | Regel | Beschreibung |")
    emit("| --- | ---: | --- | --- | --- |")
    for cs in checkstyle_issues:
        msg = cs["message"].replace("|", "\\|")
        emit(f"| `{cs['file']}` | {cs['line']} | {cs['severity']} | `{cs['source']}` | {msg} |")
else:
    emit("_Keine Checkstyle-Meldungen gefunden._")
emit("")

emit("## 5. Generierte Detailberichte")
emit("")
emit(f"- **Markdown-Zusammenfassung:** `target/metrics-report.md`")
emit(f"- **JaCoCo HTML-Report:** `target/site/jacoco/index.html`")
emit(f"- **JaCoCo XML/CSV:** `target/site/jacoco/jacoco.xml`, `target/site/jacoco/jacoco.csv`")
emit(f"- **PMD XML-Report:** `target/pmd.xml`")
emit(f"- **CPD XML-Report:** `target/cpd.xml`")
emit(f"- **Checkstyle XML-Report:** `target/checkstyle-result.xml`")

report_text = "\n".join(out_lines) + "\n"
target_dir.mkdir(parents=True, exist_ok=True)
report_md_path.write_text(report_text, encoding="utf-8")
print("")
print(report_text)
PYEOF
