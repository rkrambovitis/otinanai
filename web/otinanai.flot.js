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

        if (number < 10 )
                result = result.toFixed(2);
        else if (Math.abs(result) < 10 )
                result = result.toFixed(1);
        else if (result.toString().length > 5)
                result = result.toFixed(2);

        return result+suffix;
};

function toggleDashboard(input, board) {
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(input+" --toggleDashboard "+board);
	xhttp.open("GET", urlToStar, true);
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			console.log(xhttp.responseText);
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
		var storeLink = "";

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
					//storeLink += key.replace("\"", "") +" ";
					storeLink += key +" ";
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

			$("<span class='starGraph fa fa-star-o fa-2x' onclick=\"toggleDashboard('"+storeLink+"', 'test')\"></span>").appendTo("#placeholder_"+ph);

			ph++;
			data = [];
			ymax = 0;
			ysum = 0;
			stacked = stackedDefault;
			spikes = spikesDefault;
			storeLink = "";
		});
	};
	drawGraphs();
});
