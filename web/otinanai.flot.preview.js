$(function() {
        var xmin = null;
        var xmax = null;
   $.each(datasets, function(key) {
      $("#"+key).bind("plothover", function (event, pos, item) {
	      if (item) {
		      var x = item.datapoint[0], y = item.datapoint[1];
		      $("#popup").html(addSuffix(y))
			      .css({top: item.pageY+10, left: item.pageX-40})
			      .fadeIn(10);
	      } else {
		      $("#popup").hide();
	      }
      });
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

   });

   $("<div id='popup'></div>").css({
	   position: "absolute",
	   display: "none",
	   border: "1px solid #fdd",
	   padding: "2px",
	   "background-color": "#fee",
	   opacity: 0.80
   }).appendTo("body");


   function drawGraphs() {
      $.each(datasets, function(key, val) {
         $.plot($("#"+key), [val] , {
		 xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
		 series: { lines: {show: true, fill: false}},
		 yaxis: { show: true, tickFormatter: addSuffix, min: null, max: (showSpikes ? null : datasets[key]['nn'])},
		 grid: { hoverable: true, autoHighlight: false, clickable: true },
		 crosshair: { mode: "y" },
		 events: { data: marktext },
		 legend: { show: true, position: "nw" },
                 selection: { mode: "x" }
         });
      });
   };

   drawGraphs();
});
