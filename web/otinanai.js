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
