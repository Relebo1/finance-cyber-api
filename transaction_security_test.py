#!/usr/bin/env python3
"""Read-only SQL injection assessment for the local Finance practice API."""
from __future__ import annotations

import argparse
import errno
import json
import socket
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

BASE = "http://127.0.0.1:8080"  # Deliberately fixed: never accept a remote target.
TIMEOUT = 5
REPORT = Path("transaction_security_report.md")
STATE = Path("transaction_security_history.json")


def parse_body(body: bytes) -> tuple[dict, str]:
    raw = body.decode("utf-8", "replace")
    try:
        val = json.loads(raw)
    except (json.JSONDecodeError, ValueError):
        return {}, raw[:250].replace("\n", " ")
    if isinstance(val, list):
        return {"result_count": len(val), "sample": val[:2]}, raw[:250].replace("\n", " ")
    if isinstance(val, dict):
        fields = {k: val[k] for k in ("status", "error", "path", "content", "pageable", "totalElements", "numberOfElements", "sort", "id", "username", "displayName") if k in val}
        if isinstance(val.get("content"), list):
            fields["result_count"] = len(val["content"])
            fields["sample"] = val["content"][:2]
        return fields, raw[:250].replace("\n", " ")
    return {}, raw[:250].replace("\n", " ")


def send(method: str, path: str, params: dict | None = None, payload: dict | None = None) -> dict:
    # Guard against accidental expansion beyond the explicitly authorized loopback service.
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    body = json.dumps(payload).encode() if payload is not None else None
    headers = {"Accept": "application/json", "User-Agent": "local-transaction-security-check/2.0"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as res:
            content = res.read()
            code, ctype, error_kind = res.status, res.headers.get("Content-Type", ""), None
    except urllib.error.HTTPError as exc:
        content = exc.read()
        code, ctype, error_kind = exc.code, exc.headers.get("Content-Type", ""), "HTTP application response"
    except urllib.error.URLError as exc:
        reason = exc.reason
        if isinstance(reason, PermissionError) or (isinstance(reason, OSError) and getattr(reason, "errno", None) == 1):
            error_kind = "Python execution environment restriction"
        elif isinstance(reason, socket.timeout) or isinstance(reason, TimeoutError):
            error_kind = "Connection timeout"
        elif isinstance(reason, ConnectionRefusedError) or (isinstance(reason, OSError) and getattr(reason, "errno", None) in (errno.ECONNREFUSED, 61, 111)):
            error_kind = "Connection refused"
        else:
            error_kind = "Local connection error"
        return {"status": None, "elapsed_ms": round((time.perf_counter()-started)*1000, 1), "response_bytes": 0,
                "content_type": "", "fields": {}, "excerpt": str(reason)[:250], "transport": error_kind}
    except (socket.timeout, TimeoutError) as exc:
        return {"status": None, "elapsed_ms": round((time.perf_counter()-started)*1000, 1), "response_bytes": 0,
                "content_type": "", "fields": {}, "excerpt": str(exc)[:250], "transport": "Connection timeout"}
    except Exception as exc:
        return {"status": None, "elapsed_ms": round((time.perf_counter()-started)*1000, 1), "response_bytes": 0,
                "content_type": "", "fields": {}, "excerpt": str(exc)[:250], "transport": "Python execution environment restriction" if isinstance(exc, PermissionError) else type(exc).__name__}
    fields, excerpt = parse_body(content)
    return {"status": code, "elapsed_ms": round((time.perf_counter()-started)*1000, 1), "response_bytes": len(content),
            "content_type": ctype, "fields": fields, "excerpt": excerpt, "transport": error_kind or "HTTP response"}


def add(results: list, technique: str, repo: str, method: str, path: str, params: dict | None,
        payload: dict | None, expected: str, observation: dict, assessment: str | None = None) -> dict:
    row = {"technique": technique, "repository": repo, "endpoint": path, "method": method,
           "input": {"query": params, "json": payload}, "expected": expected, "actual": observation.get("fields") or observation.get("excerpt"),
           **observation}
    row["assessment"] = assessment or ("INCONCLUSIVE" if observation["status"] is None or observation["status"] >= 400 else "PASS")
    results.append(row)
    return row


def get(results, technique, repo, path, params, expected):
    return add(results, technique, repo, "GET", path, params, None, expected, send("GET", path, params))


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--history", default=str(STATE), help="JSON history used to compare repeat runs (default: %(default)s)")
    args = ap.parse_args()
    history_path = Path(args.history)
    previous = {}
    if history_path.exists():
        try:
            previous = json.loads(history_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            previous = {}

    results: list[dict] = []
    # This benign request is only a connectivity check; permission errors are not app outages.
    health = add(results, "Connectivity", "TransactionController", "GET", "/api/transactions", None, None,
                 "HTTP response from local app", send("GET", "/api/transactions", {"size": 1}))
    if health["status"] is None:
        statuses = {name: "NOT TESTED" for name in TECHNIQUES}
        statuses["Connectivity"] = health["transport"]
        # Keep old historical evidence; current probes are explicitly marked unrun.
        write_report(results, statuses, previous, "Connection check did not reach an HTTP application response; remaining probes were skipped.")
        print(f"{health['transport']}; assessment probes skipped. Report: {REPORT}")
        print("Run directly in macOS Terminal as described in TRANSACTION_SECURITY_TEST_README.md.")
        return

    # Boolean + malformed quote coverage for all three unsafe repository families.
    tx_base = get(results, "Boolean-based SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "groceries"}, "Matching fictional transaction rows")
    tx_taut = get(results, "Boolean-based SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "%' OR 1=1 -- "}, "Same matching rows as baseline")
    tx_true = get(results, "Blind SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "groceries%' AND '1'='1' -- "}, "Same result count as baseline")
    tx_false = get(results, "Blind SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "groceries%' AND '1'='2' -- "}, "Zero or fewer results than true condition")
    cat_base = get(results, "Boolean-based SQL injection", "UnsafeCategoryRepository", "/api/categories/search", {"name": "Groceries"}, "Matching fictional categories")
    cat_taut = get(results, "Boolean-based SQL injection", "UnsafeCategoryRepository", "/api/categories/search", {"name": "%' OR 1=1 -- "}, "Only matching categories")
    user_base = get(results, "Boolean-based SQL injection", "UnsafeUserRepository", "/api/users/lookup", {"username": "labuser"}, "Fictional labuser record")
    user_taut = get(results, "Boolean-based SQL injection", "UnsafeUserRepository", "/api/users/lookup", {"username": "' OR '1'='1' -- "}, "No unintended user record returned")

    # The credential repository is called by this actual POST endpoint. Password remains fictional.
    login = add(results, "Boolean-based SQL injection", "UnsafeUserRepository", "POST", "/api/auth/login", None,
                {"username": "no-such-lab-user", "password": "' OR '1'='1' -- "},
                "Invalid-credentials response; no user returned",
                send("POST", "/api/auth/login", payload={"username": "no-such-lab-user", "password": "' OR '1'='1' -- "}))

    # Quote/error cases are URL-encoded by urlencode. HTTP errors are recorded, never sufficient alone for VULNERABLE.
    for repo, path, key, value in [
        ("UnsafeTransactionRepository", "/api/transactions/search", "q", "'"),
        ("UnsafeCategoryRepository", "/api/categories/search", "name", "'"),
        ("UnsafeUserRepository", "/api/users/lookup", "username", "'"),
    ]:
        get(results, "Error-based SQL injection", repo, path, {key: value}, "Validation or generic error; no SQL details exposed")

    # Read-only UNION probes use only NULL expressions and the select widths established from source.
    for repo, path, key, cols in [("UnsafeTransactionRepository", "/api/transactions/search", "q", 11),
                                  ("UnsafeCategoryRepository", "/api/categories/search", "name", 5),
                                  ("UnsafeUserRepository", "/api/users/lookup", "username", 4)]:
        payload = "%' UNION SELECT " + ",".join(["NULL"] * cols) + " -- "
        get(results, "UNION-based SQL injection", repo, path, {key: payload}, f"No injected row; read-only UNION with {cols} NULL columns")

    # One bounded 1 second database delay probe; avoid repetitions and compare against a baseline.
    timing_base = get(results, "Time-based SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "groceries"}, "Normal baseline latency")
    timing_probe = get(results, "Time-based SQL injection", "UnsafeTransactionRepository", "/api/transactions/search", {"q": "%' AND IF(1=1,SLEEP(1),0) AND 'x'='x' -- "}, "No intentional delay on safe parameterized query")

    # The transaction list has actual type/q/page/size parameters; filter uses unsafe dynamic concatenation.
    filtered_base = get(results, "Dynamic SQL injection", "UnsafeTransactionRepository", "/api/transactions", {"type": "EXPENSE", "size": "100"}, "Expense rows only")
    filtered = get(results, "Dynamic SQL injection", "UnsafeTransactionRepository", "/api/transactions", {"type": "EXPENSE' OR '1'='1' -- ", "size": "100"}, "Expense rows only; arbitrary caller SQL has no effect")
    # sort is not a controller parameter, but query params are accepted/ignored by Spring MVC.
    get(results, "Dynamic SQL injection", "TransactionController", "/api/transactions", {"sort": "__invalid_field__,sideways"}, "Undeclared sort parameter ignored; no dynamic sort feature")

    # Source review only. Writes are deliberately avoided to keep this run read-only.
    add(results, "Second-order SQL injection", "TransactionService / CategoryService", "NOT TESTED", "/api/transactions (POST); /api/categories (POST)", None, None,
        "Would require storing a value; not exercised. Source review found no stored-value-to-dynamic-SQL path in inspected transaction/category flows.",
        {"status": None, "elapsed_ms": None, "response_bytes": 0, "content_type": "", "fields": {}, "excerpt": "No write request sent", "transport": "Not tested"}, "NOT TESTED")

    # Per-technique conclusions require data differences, not merely HTTP status codes.
    counts = lambda row: row.get("fields", {}).get("result_count")
    bool_vuln = (tx_base["status"] == tx_taut["status"] == 200 and counts(tx_base) is not None and counts(tx_taut) is not None and counts(tx_taut) > counts(tx_base))
    blind_vuln = (tx_true["status"] == tx_false["status"] == 200 and counts(tx_true) is not None and counts(tx_false) is not None and counts(tx_true) > counts(tx_false))
    cat_vuln = (cat_base["status"] == cat_taut["status"] == 200 and counts(cat_base) is not None and counts(cat_taut) is not None and counts(cat_taut) > counts(cat_base))
    user_vuln = (user_base["status"] == user_taut["status"] == 200 and user_base.get("fields", {}).get("username") == "labuser")
    login_vuln = login["status"] == 200 and "user" in login.get("fields", {}) and "error" not in login.get("fields", {})
    for row in results:
        if row["technique"] == "Boolean-based SQL injection" and row["repository"] == "UnsafeTransactionRepository" and row["input"].get("query", {}).get("q", "").startswith("%'"):
            row["assessment"] = "VULNERABLE" if bool_vuln else "INCONCLUSIVE"
        if row["technique"] == "Boolean-based SQL injection" and row["repository"] == "UnsafeCategoryRepository" and row["input"].get("query", {}).get("name", "").startswith("%'"):
            row["assessment"] = "VULNERABLE" if cat_vuln else "INCONCLUSIVE"
        if row["technique"] == "Boolean-based SQL injection" and row["repository"] == "UnsafeUserRepository":
            row["assessment"] = "VULNERABLE" if user_vuln or login_vuln else "INCONCLUSIVE"
        if row["technique"] == "Blind SQL injection" and row is tx_false:
            row["assessment"] = "VULNERABLE" if blind_vuln else "INCONCLUSIVE"
        if row["technique"] == "Time-based SQL injection" and row is timing_probe:
            delta = None if timing_probe["elapsed_ms"] is None or timing_base["elapsed_ms"] is None else timing_probe["elapsed_ms"] - timing_base["elapsed_ms"]
            row["assessment"] = "VULNERABLE" if delta is not None and delta >= 800 else "INCONCLUSIVE"
        if row["technique"] == "Dynamic SQL injection" and row is filtered:
            row["assessment"] = "VULNERABLE" if filtered["status"] == filtered_base["status"] == 200 and counts(filtered) is not None and counts(filtered_base) is not None and counts(filtered) > counts(filtered_base) else "INCONCLUSIVE"

    current = {t: any(r["technique"] == t and r["assessment"] == "VULNERABLE" for r in results) for t in TECHNIQUES}
    statuses = {}
    for t in TECHNIQUES:
        tested = [r for r in results if r["technique"] == t]
        if current[t]:
            statuses[t] = "VULNERABLE"
        elif not tested or all(r["assessment"] == "NOT TESTED" for r in tested):
            statuses[t] = "NOT TESTED"
        elif any(r["status"] is None for r in tested):
            statuses[t] = "INCONCLUSIVE"
        elif t == "Second-order SQL injection":
            statuses[t] = "NOT TESTED"
        else:
            statuses[t] = "PASS" if all(r["status"] is not None and r["status"] < 500 for r in tested) else "INCONCLUSIVE"
    previous_confirmed = previous.get("confirmed", {})
    remediation = {}
    for t, was in previous_confirmed.items():
        if was:
            remediation[t] = "STILL REPRODUCIBLE" if current.get(t) else ("NO LONGER REPRODUCIBLE (PASS)" if statuses.get(t) == "PASS" else "INCONCLUSIVE — prior finding not reproduced, but current checks were inconclusive")
    if previous_confirmed:
        statuses["SQL injection remediation verification"] = "VULNERABLE" if any(v == "STILL REPRODUCIBLE" for v in remediation.values()) else ("PASS" if remediation and all("NO LONGER REPRODUCIBLE" in v for v in remediation.values()) else "INCONCLUSIVE")
    history = {"updated": datetime.now(timezone.utc).isoformat(timespec="seconds"), "confirmed": current,
               "last_statuses": statuses, "previous_remediation_comparison": remediation}
    history_path.write_text(json.dumps(history, indent=2), encoding="utf-8")
    write_report(results, statuses, previous, None, remediation)
    print(f"Assessment complete. Report: {REPORT}; comparison state: {history_path}")


TECHNIQUES = ["Boolean-based SQL injection", "UNION-based SQL injection", "Error-based SQL injection",
              "Blind SQL injection", "Time-based SQL injection", "Second-order SQL injection",
              "Dynamic SQL injection", "SQL injection remediation verification"]


def write_report(results: list, statuses: dict, previous: dict, note: str | None, remediation: dict | None = None) -> None:
    lines = ["# Transaction SQL injection assessment", "", f"Generated: {datetime.now(timezone.utc).isoformat(timespec='seconds')}",
             "", "Target: `http://127.0.0.1:8080` only.", "", "## Status definitions", "",
             "- **PASS**: test ran and evidence did not reproduce the tested vulnerability.",
             "- **FAIL**: a required check or expected safe behavior failed (not synonymous with HTTP 4xx/5xx).",
             "- **VULNERABLE**: response evidence demonstrates changed query logic or a controlled delay.",
             "- **INCONCLUSIVE**: the request did not reach a meaningful response or evidence cannot establish SQL behavior.",
             "- **NOT TESTED**: intentionally omitted, usually because it would require a write.", "", "## Technique summary", ""]
    for technique in TECHNIQUES:
        lines.append(f"- **{statuses.get(technique, 'NOT TESTED')} — {technique}**")
    if note:
        lines += ["", f"**Run note:** {note}"]
    if remediation:
        lines += ["", "## Remediation verification", ""]
        lines.extend(f"- **{status}** for previously confirmed {technique}." for technique, status in remediation.items())
    if previous.get("confirmed"):
        lines += ["", "## Previous confirmed evidence", "", "The previous run's confirmed-technique flags are retained in `transaction_security_history.json` and compared on each successful run.", ""]
        lines += [f"- {k}: {'confirmed' if v else 'not confirmed'}" for k, v in previous["confirmed"].items()]
    lines += ["", "## Individual test evidence", "", "| Technique | Endpoint | Method | Input | HTTP | Time ms | Bytes | Expected behaviour | Actual behaviour / evidence | Assessment |", "|---|---|---|---|---:|---:|---:|---|---|---|"]
    for r in results:
        inp = json.dumps(r["input"], ensure_ascii=False, separators=(",", ":"))
        actual = r.get("actual")
        if r.get("status") is None:
            actual = f"{r.get('transport')}: {r.get('excerpt')}"
        cells = [r["technique"], r["endpoint"], r["method"], inp, str(r.get("status")), str(r.get("elapsed_ms")), str(r.get("response_bytes")), r["expected"], json.dumps(actual, ensure_ascii=False, separators=(",", ":")) if not isinstance(actual, str) else actual, r["assessment"]]
        lines.append("| " + " | ".join(str(c).replace("|", "\\|").replace("\n", " ") for c in cells) + " |")
    lines += ["", "## Endpoint/source map", "",
        "- `UnsafeTransactionRepository`: `GET /api/transactions/search?q=...`, filtered `GET /api/transactions?type=...&q=...`, and `GET /api/transactions/{id}`. `TransactionController` declares `type`, `q`, `startDate`, `endDate`, `page`, `size`; it declares no sorting parameters.",
        "- `UnsafeCategoryRepository`: `GET /api/categories/search?name=...` (also `/api/category/search`).",
        "- `UnsafeUserRepository`: `GET /api/users/lookup?username=...` and `POST /api/auth/login` with JSON `username` and `password`.",
        "- Stored transaction/category data is created through `POST /api/transactions` and `POST /api/categories`; these write endpoints are not called by this read-only runner.",
        "", "## Interpretation and fixes", "",
        "The reviewed repositories concatenate request values into JDBC `Statement` SQL, so source review confirms injection-prone construction in transaction search/filters/raw ID, category search, and user lookup/login. Runtime confirmation is recorded separately and requires an observable result difference; 400/500 alone is never sufficient. Error-based SQL requires database-specific diagnostic evidence, and HTTP behavior alone may remain inconclusive.",
        "", "Use `PreparedStatement` placeholders or Spring Data JPA binding for all values. Validate IDs and filter values. If sorting is implemented, allow-list property names/directions. Keep client errors generic and log detailed exceptions server-side. Add authentication and per-user authorization before using real data.",
        "", "Second-order testing is NOT TESTED because proving it dynamically requires creating a stored value. Source review found no inspected transaction/category stored-value-to-SQL-concatenation path. The script sends only local requests, does no stacked statements, data writes, enumeration, or external requests; the timing check is one conditional approximately one-second delay probe.", ""]
    REPORT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    main()
