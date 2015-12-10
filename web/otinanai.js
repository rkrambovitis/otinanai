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
	document.cookie = "dashboard="+dashName+";"; 
}

function getDashboard() {
	var dashName = getCookie("dashboard");
	if (dashName == "") {
		setDashboard("test");
		dashName = "test";
	}
	return dashName;
}
