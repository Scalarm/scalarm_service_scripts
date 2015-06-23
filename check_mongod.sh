( ps aux | grep [m]ongod ) > /dev/null &&
	echo "OK" && exit 0 ||
	echo "NOT WORKING" && exit 1