$(function() {
	$(".helpTrigger").hover(function() {
		var tooltip = $("> div", this).show();
		var pos = tooltip.offset();
		tooltip.hide();
		var right = pos.left + tooltip.width();
		var pageWidth = $(document).width();
		if (pos.left < 0) {
			tooltip.css("marginLeft", "+=" + (-pos.left) + "px");
		}
		else if (right > pageWidth) {
			tooltip.css("marginLeft", "-=" + (right - pageWidth));
		}
		tooltip.fadeIn();
	}, function() {
		$("> div", this).fadeOut(function() {$(this).css("marginLeft", "");});
	});
});

function addSuffix(number) {
        var suffix = "";
        var result = number;
        if (number > 500000000000) {
                suffix = "P";
                result = number / 1000000000000;
        } else if (number > 500000000) {
                suffix = "G";
                result = number / 1000000000;
        } else if (number > 500000) {
                suffix = "M";
                result = number / 1000000;
        } else if (number > 500) {
                suffix = "k";
                result = number / 1000;
        }

        if (number < 1 )
                result = result.toFixed(3);
        else if (number < 10 )
                result = result.toFixed(2);
        else if (Math.abs(result) < 10 )
                result = result.toFixed(1);
        else if (result.toString().length > 5)
                result = result.toFixed(2);

        return result+suffix;
};

function toggleStar(input) {
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(input+" --toggleStar");
	xhttp.open("GET", urlToStar, true);
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if (xhttp.responseText == "true") {
				document.getElementById("star").className = "fa fa-star fa-2x";
			} else {
				document.getElementById("star").className = "fa fa-star-o fa-2x";
			}
		}
	}
	xhttp.send();
}

function runXHR(input) {
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(input);
	xhttp.open("GET", urlToStar, true);
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
                        $('#tickBox').show();
                        $('#tickBox').fadeOut("slow");
		}
	}
	xhttp.send();
}

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for(var i=0; i<ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1);
		if (c.indexOf(name) == 0) return c.substring(name.length,c.length);
	}
	return "";
}

function setDashboard(dashName) {
	var d = new Date();
	d.setTime(d.getTime() + 31104000000);
	var expires = "expires="+d.toUTCString();
	document.cookie = "dashboard="+dashName+"; " + expires;
	document.getElementById('dashSelector').style.display = "none";
	document.getElementById('currentDashboard').innerHTML=dashName;
}

function getDashboard() {
	var dashName = getCookie("dashboard");
	if (dashName == "") {
		setDashboard("test");
		dashName = "test";
	}
	return dashName;
}

function showDashSelector() {
        var foo = document.getElementById('dashSelector');
        if (foo.style.display == 'block') {
                foo.style.display = 'none';
        } else {
                foo.style.display = 'block';
        }
}
