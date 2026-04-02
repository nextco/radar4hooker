#!/usr/bin/env python3
import os
import sys


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    js_path = os.path.join(script_dir, "hooker_ui_mcp_stdio.js")
    os.execvp("node", ["node", js_path] + sys.argv[1:])


if __name__ == "__main__":
    main()
