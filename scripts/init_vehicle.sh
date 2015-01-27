echo "Initializing $1"

URL=http://$1:$2

REMOTESERVICESURL=$URL/remoteservices/v1/vehicle

VIN=3887930279
LOCKVIN=6454813533

# Turn Engine OFff
AUTH_HEADER="Authorization: Basic cHJvdmlkZXI6MTIzNA=="
USER_HEADER="Username: provider"
PASSWORD_HEADER="Password: 1234"



echo "Turning off engine..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/engineOff/$VIN -s > /dev/null

echo "Turning off alarm..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/alarmOff/$VIN -s > /dev/null

echo "Closing trunk..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/closeTrunk/$VIN -s > /dev/null

echo "Locking doors..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/lock/$VIN -s > /dev/null
