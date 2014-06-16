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
      $("#"+key).bind("plothover",  function (event, pos) {
         //debugger;
	      latestPosition = pos;
	      if (!updateLegendTimeout) {
		      updateLegendTimeout = setTimeout(updateLegend(key), 50);
		   //   updateLegend(key);
	      }
         updateLegendTimeout = null;
      });
   });

   function updateLegend(key) {
      //console.log(key);
      var pos = latestPosition;
      var axes = handles[key].getAxes();
      if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
         return;
      }
      var j = 0;
      $.each(datasets, function(key, val) {
         if (val.data.length <= 2) {
         } else {
            for (j = 0; j < val.data.length; ++j) {
               if (val.data[j][0] < pos.x) {
                  break;
               }
            }
            var y = val.data[j][1];
            $("#"+key+" .legendLabel").text(key + " = " + addSuffix(y));
         }
      });
   };

   function drawGraphs() {
      $.each(datasets, function(key, val) {
         handles[key] =$.plot($("#"+key), [val], {
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
            legend: { show: "true", position: "nw" },
            series: { lines: {show: true, fill: false}},
            crosshair: { mode: "x"},
            yaxis: { show: true, tickFormatter: addSuffix, min: null, max: null },
            grid: { hoverable: true, autoHighlight: false, clickable: true},
            selection: { mode: "x" }
         });
      });
   };

   drawGraphs();
});
