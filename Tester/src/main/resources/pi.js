/**
 * variable x is expected to be global
 */
function calcPi() {
	var n = x - (x % 2);

	var num = 1;
	var den = n + 1;

	for (var i = 1; i < n; i = i + 2) {
		num = num * (i + 1) * (i + 1);
		num = num / (i * i);
	}

	return (num / den) * 2;
}
