"""Start an installed NeoForge dedicated server headless, run console commands, save and stop.

  python -I run_server.py <server dir> <java exe> <neoforge version> <log> [commands file] [settle seconds]

Waits for "Done (", sends each command line, waits <settle> seconds (default 30) so forced chunks
load and tick, then "save-all flush" and "stop". Exit code 0 only on a clean save and shutdown.
"""
import subprocess
import sys
import threading
import time

server, java, neo, log_path = sys.argv[1:5]
commands = open(sys.argv[5], encoding="utf-8").read().splitlines() if len(sys.argv) > 5 and sys.argv[5] else []
settle = float(sys.argv[6]) if len(sys.argv) > 6 else 30.0

proc = subprocess.Popen([java, "@user_jvm_args.txt", f"@libraries/net/neoforged/neoforge/{neo}/win_args.txt",
                         "nogui"], cwd=server, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
lines, seen = [], threading.Event()
saved, done = threading.Event(), threading.Event()


def pump():
    with open(log_path, "w", encoding="utf-8") as log:
        for line in proc.stdout:
            log.write(line)
            log.flush()
            lines.append(line)
            if "Done (" in line:
                done.set()
            if "Saved the game" in line:
                saved.set()


threading.Thread(target=pump, daemon=True).start()


def send(cmd):
    proc.stdin.write(cmd + "\n")
    proc.stdin.flush()


if not done.wait(600):
    proc.kill()
    sys.exit("server did not reach Done")
for cmd in commands:
    if cmd.strip():
        send(cmd)
time.sleep(settle)
send("save-all flush")
ok = saved.wait(300)
send("stop")
try:
    proc.wait(300)
except subprocess.TimeoutExpired:
    proc.kill()
    sys.exit("server did not stop")
time.sleep(1)
errors = [l for l in lines if ("ERROR" in l or "Exception" in l) and "cesg" in l.lower()]
print(f"done=1 saved={int(ok)} exit={proc.returncode} cesg_error_lines={len(errors)}")
for l in errors[:20]:
    print("  " + l.rstrip())
sys.exit(0 if ok and proc.returncode == 0 else 1)
