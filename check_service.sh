# $1 - address
function check {
	STATUS=`curl -skw "%{http_code}" https://$1/status -o /dev/null`
	echo "$STATUS"
	if [ $STATUS == "200" ]; then
		exit 0
	else
		exit 1
	fi
}

check $1
