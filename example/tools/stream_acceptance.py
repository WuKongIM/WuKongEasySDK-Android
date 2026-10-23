#!/usr/bin/env python3
"""Opt-in native black-box acceptance; owns only its temporary server and fixture."""
import argparse
import http.server
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import urllib.request
import urllib.error


def port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-bin', required=True, type=Path)
    parser.add_argument('--adb', default=str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    parser.add_argument('--serial', default=os.getenv('ANDROID_SERIAL'))
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    output = args.output or Path(tempfile.mkdtemp(prefix='wk-android-stream-'))
    output.mkdir(parents=True, exist_ok=True)
    adb = [args.adb] + (['-s', args.serial] if args.serial else [])
    subprocess.run(adb + ['get-state'], check=True, capture_output=True)
    api, raft, ws, model_port, proxy_port = port(), port(), port(), port(), port()
    metrics = {'calls': [], 'cancelled': 0, 'history_reads': 0, 'eventsync_reads': 0}

    class Fixture(http.server.BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'

        def log_message(self, *_):
            pass

        def do_GET(self):
            data = json.dumps(metrics).encode()
            self.send_response(200); self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)

        def do_POST(self):
            size = int(self.headers.get('Content-Length', '0'))
            if size > 131072:
                self.send_error(413)
                return
            request = json.loads(self.rfile.read(size))
            model = request.get('model', '')
            # Record structural evidence only, never headers, credentials or prompts.
            metrics['calls'].append({'model': model, 'roles': [m['role'] for m in request.get('messages', [])]})
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Connection', 'close')
            self.end_headers()
            self.close_connection = True

            def packet(delta):
                value = json.dumps({'choices': [{'index': 0, 'delta': {'content': delta}}]}, ensure_ascii=False)
                data = ('data: ' + value + '\r\n\r\n').encode()
                # Split UTF-8 and SSE delimiters across network writes.
                for i in range(0, len(data), 3):
                    self.wfile.write(data[i:i + 3])
                    self.wfile.flush()
                time.sleep(0.08)
            try:
                if model == 'fixture-error':
                    packet('保留部分内容🙂')
                    self.wfile.write(b'data: {"error":{"message":"private upstream body"}}\n\n')
                elif model == 'fixture-truncated':
                    packet('断流前内容')
                elif model == 'fixture-slow':
                    for _ in range(300):
                        packet('慢🙂')
                    self.wfile.write(b'data: [DONE]\n\n')
                else:
                    for chunk in ['模型回复', '🙂，', '分段到达。']:
                        packet(chunk)
                    # A valid multiline JSON SSE event and an explicit terminal marker.
                    self.wfile.write(b'data: {"choices":\ndata: [{"index":0,"delta":{},"finish_reason":"stop"}]}\n\ndata: [DONE]\n\n')
                self.wfile.flush()
            except (BrokenPipeError, ConnectionResetError):
                metrics['cancelled'] += 1

    class ProductProxy(http.server.BaseHTTPRequestHandler):
        def log_message(self, *_):
            pass

        def forward(self):
            if self.path.startswith('/channel/messagesync'):
                metrics['history_reads'] += 1
            if self.path.startswith('/message/eventsync'):
                metrics['eventsync_reads'] += 1
            body = None
            if self.command == 'POST':
                body = self.rfile.read(int(self.headers.get('Content-Length', '0')))
            request = urllib.request.Request(f'http://127.0.0.1:{api}' + self.path, data=body,
                                             headers={'Content-Type': 'application/json'}, method=self.command)
            try:
                response = urllib.request.urlopen(request, timeout=15)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                data = response.read(2 * 1024 * 1024)
                self.send_response(response.status); self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)

        do_GET = forward
        do_POST = forward

    proxy = http.server.ThreadingHTTPServer(('127.0.0.1', proxy_port), ProductProxy)
    threading.Thread(target=proxy.serve_forever, daemon=True).start()
    fixture = http.server.ThreadingHTTPServer(('127.0.0.1', model_port), Fixture)
    threading.Thread(target=fixture.serve_forever, daemon=True).start()
    config = output / 'wukongim.toml'
    config.write_text(f'''[node]
id = 1
data_dir = "{output}/data"
[cluster]
id = "android-stream-acceptance"
listen_addr = "127.0.0.1:{raft}"
nodes = [{{id = 1, addr = "127.0.0.1:{raft}"}}]
initial_slot_count = 8
hash_slot_count = 256
slot_replica_n = 1
[api]
listen_addr = "127.0.0.1:{api}"
external_ws_addr = "ws://127.0.0.1:{ws}"
[manager]
listen_addr = "127.0.0.1:0"
[gateway]
token_auth_on = true
listeners = [{{name = "ws", network = "websocket", address = "127.0.0.1:{ws}", transport = "gnet", protocol = "wsmux"}}]
[log]
level = "warn"
dir = "{output}/logs"
''')
    environment = {k: v for k, v in os.environ.items() if not k.startswith('WK_')}
    server_log = (output / 'server.log').open('w')
    server = subprocess.Popen([str(args.server_bin.resolve()), '-config', str(config)], cwd=output,
                              env=environment, stdout=server_log, stderr=subprocess.STDOUT)
    reverses = []
    try:
        deadline = time.monotonic() + 45
        while True:
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{api}/readyz', timeout=1) as response:
                    if response.status == 200:
                        break
            except Exception:
                if time.monotonic() > deadline or server.poll() is not None:
                    raise RuntimeError('Temporary cluster did not become ready; inspect server.log')
                time.sleep(0.1)
        for number in [proxy_port, ws, model_port]:
            subprocess.run(adb + ['reverse', f'tcp:{number}', f'tcp:{number}'], check=True, capture_output=True)
            reverses.append(number)
        for apk in ['example/build/outputs/apk/debug/example-debug.apk',
                    'example/build/outputs/apk/androidTest/debug/example-debug-androidTest.apk']:
            subprocess.run(adb + ['install', '-r', str(repo / apk)], check=True, capture_output=True)
        command = adb + ['shell', 'am', 'instrument', '-w', '-e', 'class', 'com.githubim.easysdk.example.StreamingDemoTest',
                         '-e', 'streamApiUrl', f'http://127.0.0.1:{proxy_port}', '-e', 'streamModelUrl', f'http://127.0.0.1:{model_port}',
                         'com.githubim.easysdk.example.test/androidx.test.runner.AndroidJUnitRunner']
        result = subprocess.run(command, capture_output=True, text=True, timeout=300)
        (output / 'instrumentation.txt').write_text(result.stdout + result.stderr)
        subprocess.run(adb + ['pull', '/sdcard/Android/data/com.githubim.easysdk.example/files/stream-acceptance', str(output)],
                       capture_output=True, check=True)
        report_file = output / 'stream-acceptance/report.json'
        metadata = {'sdk_revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip(),
                    'sdk_worktree_dirty': bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=repo, text=True).strip()),
                    'device_api': subprocess.check_output(adb + ['shell', 'getprop', 'ro.build.version.sdk'], text=True).strip()}
        report = json.loads(report_file.read_text())
        deadline = time.monotonic() + 3
        while metrics['cancelled'] < 1 and time.monotonic() < deadline:
            time.sleep(0.1)
        report['fixture'] = metrics
        report['environment'] = metadata
        context_verified = any('assistant' in c['roles'] and c['roles'][-1:] == ['user'] for c in metrics['calls'])
        if context_verified:
            report['checks'].append('fixture confirms multi-turn context')
        report['passed'] = report['passed'] and result.returncode == 0 and 'OK (1 test)' in result.stdout and metrics['cancelled'] >= 1 and metrics['eventsync_reads'] == 0 and context_verified
        report_file.write_text(json.dumps(report, indent=2, ensure_ascii=False))
        print(json.dumps({'passed': report['passed'], 'artifacts': str(output)}, ensure_ascii=False))
        if not report['passed']:
            raise RuntimeError('Acceptance failed; inspect instrumentation.txt and report.json')
    finally:
        proxy.shutdown(); proxy.server_close()
        fixture.shutdown(); fixture.server_close()
        for number in reverses:
            subprocess.run(adb + ['reverse', '--remove', f'tcp:{number}'], capture_output=True)
        server.terminate()
        try:
            server.wait(timeout=5)
        except subprocess.TimeoutExpired:
            server.kill(); server.wait()
        server_log.close()


if __name__ == '__main__':
    main()
