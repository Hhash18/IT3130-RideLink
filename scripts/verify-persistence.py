#!/usr/bin/env python3
"""Check the exact Postman run's records directly in local MySQL without printing credentials."""
import json, sys, uuid
from pathlib import Path
from local import LOCAL, sql
path=Path(sys.argv[1]) if len(sys.argv)>1 else LOCAL/'RideLink-Local.postman_environment.json'
values={item['key']:item['value'] for item in json.loads(path.read_text())['values']}
def binary(name):return "UNHEX('"+uuid.UUID(values[name]).hex+"')"
def number(name):return str(int(values[name]))
checks={
 'Account and hashed password': f"SELECT COUNT(*) FROM ridelink_account_db.accounts WHERE id={binary('passenger_account_id')} AND role='PASSENGER' AND password LIKE '$2%';",
 'Driver with assigned vehicle':f"SELECT COUNT(*) FROM ridelink_driver_db.drivers WHERE id={number('driver_id')} AND vehicle_id={number('vehicle_id')};",
 'Vehicle':f"SELECT COUNT(*) FROM ridelink_driver_db.vehicles WHERE id={number('vehicle_id')};",
 'Completed ride and fare snapshot':f"SELECT COUNT(*) FROM ridelink_ride_db.rides WHERE id={binary('ride_id')} AND status='COMPLETED' AND final_fare=945 AND final_fare_id={binary('fare_id')};",
 'Cancelled ride':f"SELECT COUNT(*) FROM ridelink_ride_db.rides WHERE id={binary('cancel_ride_id')} AND status='CANCELLED';",
 'Final fare':f"SELECT COUNT(*) FROM ridelink_fare_payment_db.fares WHERE id={binary('fare_id')} AND total=945;",
 'Successful payment with receipt':f"SELECT COUNT(*) FROM ridelink_fare_payment_db.payments WHERE id={binary('payment_id')} AND status='SUCCEEDED' AND receipt_number IS NOT NULL;",
 'Failed attempt retained':f"SELECT COUNT(*) FROM ridelink_fare_payment_db.payments WHERE id={binary('failed_payment_id')} AND status='FAILED';",
}
for label,query in checks.items():
    assert sql(query)=='1',label+' missing or incorrect'
    print('PASS:',label)
assert sql(f"SELECT COUNT(*) FROM ridelink_fare_payment_db.payments WHERE fare_id={binary('fare_id')};")=='2','Duplicate payment found'
print('PASS: exactly two attempts (one failed, one succeeded), no duplicate payment')
