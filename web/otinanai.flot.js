function toggleDashboard(input, element) {
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(input+"--toggleDashboard");
	xhttp.open("GET", urlToStar, true);
	xhttp.onreadystatechange = function() {
		if (xhttp.responseText == "true") {
			element.className = "rightIcon link starGraph fa fa-star fa-2x";
		} else {
			element.className = "rightIcon link starGraph fa fa-star-o fa-2x";
		}
	}
	xhttp.send();
}

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
		var starredDefault = preStarred;
		var starred = starredDefault;
		var percentilesDefault = percentilesGraph;
		var percentiles = percentilesDefault;
		var storeLink = "";
                var singleLink = "";

		$.each(datasets, function(wrapper, graph) {
			$.each(graph, function(key, val) {
				if (key == "showSpikes") {
					spikes = val;
				} else if (key == "stackedGraph") {
					stacked = val;
				} else if (key == "percentilesGraph") {
					percentiles = val;
				} else if (key == "preStarred") {
					starred = true;
				} else {
					data.push(val);
					if (val['nn'] > ymax)
						ymax = val['nn'];
					ysum += val['nn'];
					storeLink += val['keyword'] +" ";
                                        singleLink += val['keyword']+" ";
				}
			});

			if (!spikes && stacked) 
				y = ysum;
			else if (!spikes && !stacked)
				y = ymax;
			
			$.plot(window.$("#placeholder_"+ph), data, {
				xaxis: { mode: (percentiles ? "null" : "time"), tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
				yaxis: { show: true, tickFormatter: addSuffix, min: null, max: y},
				grid: { hoverable: true, autoHighlight: false, clickable: true},
				legend: { show: true, position: "nw", sorted: (stacked ? "reverse" : false ) },
				events: { data: marktext },
				series: { stack: stacked, lines: {show: true, fill: stacked}},
                                crosshair: { mode: "y" },
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
			if (stacked) {
				storeLink +="--stack ";
                                singleLink +="--stack ";
                        }
                        else
                                singleLink +="--merge ";
                        singleLink +="%23999 ";
			$("<span class='rightIcon link starGraph fa "+ (starred ? "fa-star" : "fa-star-o") + " fa-2x' onclick=\"toggleDashboard('"+storeLink+"', this)\"></span>").appendTo("#placeholder_"+ph);
			$("<span class='rightIcon link fa fa-arrows fa-2x draggable'></span>").appendTo("#placeholder_"+ph);
			$("<span class='rightIcon link gotoGraph fa fa-arrow-right fa-2x' onclick=\"location.href = '"+singleLink+"'\"></span>").appendTo("#placeholder_"+ph);

			ph++;
			data = [];
			ymax = 0;
			ysum = 0;
			stacked = stackedDefault;
			spikes = spikesDefault;
			starred = starredDefault;
			percentiles = percentilesDefault;
			storeLink = "";
                        singleLink = "";
		});
	};
	drawGraphs();
});
