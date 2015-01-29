if [ $# -ne 2 ]
  then
    echo 
    echo "Parameter error..."
    echo 
    echo "Usage: init_vehicle.sh [luigi_host] [luigi_port]"
    echo "Example: init_vehicle.sh mafalda.hack.att.io 3000"
    echo 
    exit 0
fi

URL=http://$1:$2

REMOTESERVICESURL=$URL/remoteservices/v1/vehicle

echo "Using $REMOTESERVICESURL..."

VIN=3887930279

# Turn Engine OFff
AUTH_HEADER="Authorization: Basic cHJvdmlkZXI6MTIzNA=="
USER_HEADER="Username: provider"
PASSWORD_HEADER="Password: 1234"



echo "Turning on engine..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/engineOn/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Turning off engine..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/engineOff/$VIN -s > /dev/null

echo "Sleep 5 second..."
sleep 5

echo "Turning on alarm..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/alarmOn/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Turning off alarm..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/alarmOff/$VIN -s > /dev/null


echo "Sleep 5 second..."
sleep 5

echo "Opening trunk..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/openTrunk/$VIN -s > /dev/null

echo "Sleep 15 seconds..."
sleep 15

echo "Closing trunk..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/closeTrunk/$VIN -s > /dev/null

echo "Sleep 15 second..."
sleep 15

echo "Honk..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/honk/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Blink..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/blink/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Honk and blink..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/honkBlink/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Pickup..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/pickup/$VIN -s > /dev/null

echo "Sleep 35 seconds..."
sleep 25

echo "Unlock..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/unlock/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Park..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/park/$VIN -s > /dev/null

echo "Sleep 25 seconds..."
sleep 25

echo "Lock..."
curl -X POST --header "$AUTH_HEADER" --header "$USER_HEADER" --header "$PASSWORD_HEADER" $REMOTESERVICESURL/lock/$VIN -s > /dev/null

echo "Sleep 5 seconds..."
sleep 5

echo "Test suite finalized..."
