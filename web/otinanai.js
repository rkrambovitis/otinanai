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
	xhttp.open("GET", "?q="+input+" --toggleStar", true);
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if (xhttp.responseText == "t") {
				document.getElementById("star").className = "fa fa-star fa-2x";
			} else {
				document.getElementById("star").className = "fa fa-star-o fa-2x";
			}
		}
	}
	xhttp.send();
}
