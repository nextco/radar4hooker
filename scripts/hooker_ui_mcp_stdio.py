#!/usr/bin/env python3
import json
import os
import sys
import traceback
from copy import deepcopy
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


DEFAULT_BASE_URL = "http://10.112.101.249:8080"
DEFAULT_PROTOCOL_VERSION = "2024-11-05"


class HookerUiMcpServer:
    def __init__(self):
        self.base_url = os.environ.get("HOOKER_BASE_URL", DEFAULT_BASE_URL).rstrip("/")
        self.server_name = "hooker-ui-mcp-bridge"
        self.server_version = "0.1.0"

    def serve(self):
        while True:
            message = self._read_message()
            if message is None:
                return
            try:
                self._handle_message(message)
            except Exception as exc:
                self._write_log("request handling failed: %s\n%s" % (exc, traceback.format_exc()))
                if "id" in message:
                    self._write_error(message["id"], -32603, str(exc))

    def _handle_message(self, message):
        method = message.get("method")
        if method is None:
            if "id" in message:
                self._write_error(message["id"], -32600, "missing method")
            return

        if method == "notifications/initialized":
            return
        if method == "initialized":
            return

        msg_id = message.get("id")
        params = message.get("params") or {}

        if method == "initialize":
            client_version = params.get("protocolVersion") or DEFAULT_PROTOCOL_VERSION
            result = {
                "protocolVersion": client_version,
                "capabilities": {
                    "tools": {},
                },
                "serverInfo": {
                    "name": self.server_name,
                    "version": self.server_version,
                },
            }
            self._write_result(msg_id, result)
            return

        if method == "ping":
            self._write_result(msg_id, {})
            return

        if method == "tools/list":
            remote = self._http_get("/hooker/mcp/ui/tools")
            tools = []
            for tool in remote.get("tools", []):
                schema = deepcopy(tool.get("input_schema") or {})
                schema.setdefault("type", "object")
                schema.setdefault("properties", {})
                schema.setdefault("required", [])
                schema.setdefault("additionalProperties", False)
                tools.append({
                    "name": tool.get("name"),
                    "description": tool.get("description", ""),
                    "inputSchema": schema,
                })
            self._write_result(msg_id, {"tools": tools})
            return

        if method == "tools/call":
            name = params.get("name")
            arguments = params.get("arguments") or {}
            if not name:
                self._write_error(msg_id, -32602, "missing tool name")
                return
            remote = self._http_post("/hooker/mcp/ui/call", {
                "name": name,
                "arguments": arguments,
            })
            is_error = not bool(remote.get("ok", False))
            self._write_result(msg_id, {
                "content": [{
                    "type": "text",
                    "text": json.dumps(remote, ensure_ascii=False),
                }],
                "structuredContent": remote,
                "isError": is_error,
            })
            return

        self._write_error(msg_id, -32601, "method not found: %s" % method)

    def _http_get(self, path):
        request = Request(self.base_url + path, method="GET")
        return self._http_json(request)

    def _http_post(self, path, payload):
        body = json.dumps(payload).encode("utf-8")
        request = Request(
            self.base_url + path,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._http_json(request)

    def _http_json(self, request):
        try:
            with urlopen(request, timeout=60) as response:
                raw = response.read().decode("utf-8")
                if not raw:
                    return {}
                return json.loads(raw)
        except HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError("remote HTTP %s: %s" % (exc.code, body))
        except URLError as exc:
            raise RuntimeError("remote connection failed: %s" % exc)

    def _read_message(self):
        headers = {}
        while True:
            line = sys.stdin.buffer.readline()
            if not line:
                return None
            if line in (b"\r\n", b"\n"):
                break
            decoded = line.decode("utf-8").strip()
            if ":" not in decoded:
                continue
            key, value = decoded.split(":", 1)
            headers[key.strip().lower()] = value.strip()

        content_length = headers.get("content-length")
        if content_length is None:
            return None
        raw = sys.stdin.buffer.read(int(content_length))
        if not raw:
            return None
        return json.loads(raw.decode("utf-8"))

    def _write_result(self, msg_id, result):
        self._write_message({
            "jsonrpc": "2.0",
            "id": msg_id,
            "result": result,
        })

    def _write_error(self, msg_id, code, message):
        self._write_message({
            "jsonrpc": "2.0",
            "id": msg_id,
            "error": {
                "code": code,
                "message": message,
            },
        })

    def _write_message(self, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        header = ("Content-Length: %d\r\n\r\n" % len(body)).encode("ascii")
        sys.stdout.buffer.write(header)
        sys.stdout.buffer.write(body)
        sys.stdout.buffer.flush()

    def _write_log(self, text):
        sys.stderr.write(text.rstrip() + "\n")
        sys.stderr.flush()


def main():
    HookerUiMcpServer().serve()


if __name__ == "__main__":
    main()
