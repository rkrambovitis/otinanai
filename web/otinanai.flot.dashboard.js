$(function() {
	var xmin = null;
	var xmax = null;

	$("<div class='tooltip'></div>").css({
		position: "absolute",
		display: "none",
		border: "1px solid #fdd",
		padding: "2px",
		"background-color": "#fee",
		opacity: 0.80
	}).appendTo("body");

	function drawGraphs() {
		var ph=idx;
		var data = [];
		var ymax = 0;
		var ysum = 0;
		var y = null;
		var stackedDefault = stackedGraph;
		var stacked = stackedDefault;
		var spikesDefault = showSpikes;
		var spikes = spikesDefault;

		var text="<div>Hello World</div>";
	
		$.each(datasets, function(wrapper, graph) {
			$.each(graph, function(key, val) {
				if (key == "showSpikes") {
					spikes = val;
				} else if (key == "stackedGraph") {
					stacked = val;
				} else {
					data.push(val);
					if (val['nn'] > ymax)
						ymax = val['nn'];
					ysum += val['nn'];
				}
			});

			if (!spikes && stacked) 
				y = ysum;
			else if (!spikes && !stacked)
				y = ymax;
			
			$.plot(window.$("#placeholder_"+ph), data, {
				xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
				yaxis: { show: true, tickFormatter: addSuffix, min: null, max: y},
				grid: { hoverable: true, autoHighlight: false, clickable: true},
				legend: { show: true, position: "nw", sorted: "reverse" },
				series: { stack: stacked, lines: {show: true, fill: stacked}},
				events: { data: marktext },
				selection: { mode: "x" }
			});

				window.$("#placeholder_"+ph).bind("plothover", function (event, pos, item) {
					if (item) {
						var x = item.datapoint[0], y = item.datapoint[1];
						window.$(".tooltip").html(addSuffix(y))
							.css({top: item.pageY+10, left: item.pageX-40})
							.fadeIn(10);
					} else {
						window.$(".tooltip").hide();
					}
				});
				window.$("#placeholder_"+ph).bind("plotselected", function (event, ranges) {
						  xmin = ranges.xaxis.from;
						  xmax = ranges.xaxis.to;
						  drawGraphs();
				});
				window.$("#placeholder_"+ph).bind("plotunselected", function (event, ranges) {
						  xmin = null;
						  xmax = null;
						  drawGraphs();
				});

			ph++;
			data = [];
			ymax = 0;
			ysum = 0;
			stacked = stackedDefault;
			spikes = spikesDefault;
		});
	};
	drawGraphs();
});
