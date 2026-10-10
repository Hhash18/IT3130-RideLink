#!/usr/bin/env python3
"""Prepare only the isolated Docker local database and an ignored Postman environment."""
import json, urllib.request, urllib.error
from local import ROOT, LOCAL, config, sql
values=config()
email='admin.local@example.test'
payload={'name':'Local Admin','email':email,'password':values['LOCAL_PASSWORD'],'role':'PASSENGER'}
request=urllib.request.Request('http://localhost:8082/api/auth/register',data=json.dumps(payload).encode(),headers={'Content-Type':'application/json'})
try:
    with urllib.request.urlopen(request,timeout=10) as response: assert response.status==201
except urllib.error.HTTPError as error:
    if error.code!=409: raise
# No public admin-registration bypass: only the local Docker database owner provisions this admin.
sql("UPDATE ridelink_account_db.accounts SET role='ADMIN' WHERE email='admin.local@example.test';")
request=urllib.request.Request('http://localhost:8082/api/auth/login',data=json.dumps({'email':email,'password':values['LOCAL_PASSWORD']}).encode(),headers={'Content-Type':'application/json'})
with urllib.request.urlopen(request,timeout=10) as response:
    assert json.load(response)['role']=='ADMIN'
environment=json.loads((ROOT/'postman/RideLink-Local.postman_environment.json').read_text())
environment['name']='RideLink Local (ready to test)'
for item in environment['values']:
    if item['key']=='test_password':item['value']=values['LOCAL_PASSWORD']
path=LOCAL/'RideLink-Local.postman_environment.json'
path.write_text(json.dumps(environment,indent=2));path.chmod(0o600)
print('Import postman/RideLink-Full-Flow.postman_collection.json and .local/RideLink-Local.postman_environment.json into Postman.')
print('Run the full collection in order. The local environment contains credentials; do not commit or share it.')
