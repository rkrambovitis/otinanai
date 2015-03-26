$(function() {

   var updateLegendTimeout = null;
   var latestPosition = null;
   var myPlot = null;
   var handles = {};
   var xmin = null;
   var xmax = null;	
   $.each(datasets, function(key) {
      $("#"+key).bind("plotselected", function (event, ranges) {
         xmin = ranges.xaxis.from;
         xmax = ranges.xaxis.to;
         drawGraphs();
      });
      $("#"+key).bind("plotunselected", function (event, ranges) {
         xmin = null;
         xmax = null;
         drawGraphs();
      });
		$("#"+key).bind("plothover", function (event, pos, item) {
			if (item) {
				var x = item.datapoint[0].toFixed(2),
				y = item.datapoint[1].toFixed(2);

				$("#tooltip").html(key + ": "+ addSuffix(y))
					.css({top: item.pageY+5, left: item.pageX+5})
					.fadeIn(100);
			} else {
				$("#tooltip").hide();
			}
		});

   });

	$("<div id='tooltip'></div>").css({
		position: "absolute",
		display: "none",
		border: "1px solid #fdd",
		padding: "2px",
		"background-color": "#fee",
		opacity: 0.80
	}).appendTo("body");


	function drawGraphs() {
      $.each(datasets, function(key, val) {
         handles[key] = $.plot($("#"+key), [val], {
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
            legend: { show: "false" },
            series: { lines: {show: true, fill: false}, points: {show: true}},
            yaxis: { show: true, tickFormatter: addSuffix, min: null, max: datasets[key]['nn']},
            grid: { hoverable: true, autoHighlight: false, clickable: true},
            selection: { mode: "x" }
         });
      });
   };

   drawGraphs();
});
