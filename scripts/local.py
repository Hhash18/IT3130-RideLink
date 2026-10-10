#!/usr/bin/env python3
"""Start/stop the four local services with persistent MySQL. No external Python packages."""
import argparse, json, os, secrets, signal, socket, subprocess, time, urllib.request
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / '.local'
SERVICES = [('driver', '.', 8081), ('account', 'account-service', 8082),
            ('ride', 'ride-management-service', 8083), ('fare_payment', 'fare-payment-service', 8084)]

def config():
    LOCAL.mkdir(mode=0o700, exist_ok=True)
    path = LOCAL / 'settings.json'
    if not path.exists():
        values = {'JWT_SECRET': secrets.token_hex(32), 'MYSQL_ROOT_PASSWORD': secrets.token_hex(24),
                  'LOCAL_PASSWORD': secrets.token_urlsafe(18), 'MYSQL_PORT': 3308}
        for name, _, _ in SERVICES: values[name.upper() + '_DB_PASSWORD'] = secrets.token_hex(24)
        path.write_text(json.dumps(values, indent=2)); path.chmod(0o600)
    values = json.loads(path.read_text())
    env = LOCAL / 'mysql.env'
    env.write_text('\n'.join(f'{key}={value}' for key, value in values.items() if key.endswith('DB_PASSWORD') or key == 'MYSQL_ROOT_PASSWORD') + '\n')
    env.chmod(0o600)
    return values

def compose(*args, **kwargs):
    env = dict(os.environ, MYSQL_PORT=str(json.loads((LOCAL/'settings.json').read_text())['MYSQL_PORT']))
    return subprocess.run(['docker', 'compose', *args], cwd=ROOT, env=env, check=True, **kwargs)

def sql(statement):
    result = compose('exec', '-T', 'mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B',
                     input=statement, text=True, capture_output=True)
    return result.stdout.strip()

def stop():
    for name, _, _ in SERVICES:
        path = LOCAL / (name + '.pid')
        if path.exists():
            pid = int(path.read_text())
            # Do not signal a reused PID belonging to another application.
            command = subprocess.run(['ps','-p',str(pid),'-o','command='],capture_output=True,text=True).stdout
            if str(ROOT) in command and '.jar' in command:
                os.kill(pid, signal.SIGTERM)
            path.unlink()
    time.sleep(2)

def start(build):
    values = config()
    # Refuse to overwrite PID files or disturb applications already using these ports.
    for name, _, port in SERVICES:
        with socket.socket() as sock:
            if sock.connect_ex(('127.0.0.1', port)) == 0:
                raise SystemExit(f'Port {port} is occupied. Stop the existing service first (python3 scripts/local.py stop).')
    compose('up', '-d', '--wait', '--wait-timeout', '240')
    if build:
        for _, folder, _ in SERVICES:
            subprocess.run(['mvn','-B','-ntp','-Dmaven.resolver.transport=wagon','clean','verify'],cwd=ROOT/folder,check=True)
    started = []
    try:
        for name, folder, port in SERVICES:
            jars = list((ROOT/folder/'target').glob('*.jar'))
            if len(jars) != 1: raise RuntimeError(f'Build required for {folder}: run without --no-build')
            env = dict(os.environ, JWT_SECRET=values['JWT_SECRET'], PORT=str(port), SPRING_PROFILES_ACTIVE='local',
                       DB_URL=f"jdbc:mysql://localhost:{values['MYSQL_PORT']}/ridelink_{name}_db?connectionTimeZone=UTC",
                       DB_USERNAME='ridelink_' + name, DB_PASSWORD=values[name.upper()+'_DB_PASSWORD'])
            log = open(LOCAL/(name+'.log'),'ab')
            process = subprocess.Popen(['java','-jar',str(jars[0])],cwd=ROOT/folder,env=env,stdout=log,stderr=log,start_new_session=True)
            log.close(); (LOCAL/(name+'.pid')).write_text(str(process.pid)); started.append(process)
        deadline = time.time()+120
        pending = dict((name,port) for name,_,port in SERVICES)
        while pending and time.time()<deadline:
            for name,port in list(pending.items()):
                try:
                    with urllib.request.urlopen(f'http://localhost:{port}/v3/api-docs',timeout=2) as response:
                        if response.status==200: del pending[name]; print(f'{name}: ready on {port}', flush=True)
                except Exception: pass
            if any(p.poll() is not None for p in started): raise RuntimeError('A service exited; inspect .local/*.log')
            if pending: time.sleep(1)
        if pending: raise RuntimeError('Services not ready: '+', '.join(pending))
    except Exception:
        for process in started:
            if process.poll() is None: process.terminate()
        raise
    print('Services running. MySQL data is retained in the Docker volume.')

if __name__ == '__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('action',choices=['start','stop','status']); parser.add_argument('--no-build',action='store_true'); args=parser.parse_args()
    if args.action=='start': start(not args.no_build)
    elif args.action=='stop': stop(); print('Services stopped; MySQL and its data are retained.')
    else:
        for name,_,port in SERVICES:
            with socket.socket() as sock: print(name,port,'UP' if sock.connect_ex(('127.0.0.1',port))==0 else 'DOWN')
