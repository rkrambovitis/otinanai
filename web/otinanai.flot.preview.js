$(function() {

   var updateLegendTimeout = null;
   var latestPosition = null;
   var myPlot = null;

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
      /*
      $("#"+key).bind("plothover",  function (event, pos, item) {
	      latestPosition = pos;
	      if (!updateLegendTimeout) {
		      updateLegendTimeout = setTimeout(updateLegend(key), 50);
	      }
      });
      */
   });
/*
   $("#"+key+".legeldLabel").css('width', $("#"+key+".legeldLabel").width());
   var legends = $("#"+key+".legendLabel");

   legends.each(function () {
      $(this).css('width', $(this).width());
   });
   */


/*
   function updateLegend(foo) {
      var temp = $("#"+foo+".legendLabel");
      updateLegendTimeout = null;
      var pos = latestPosition;
      var axes = $("#"+foo).getAxes();
      if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
         return;
      }
      var j,i = 0;
      $.each(datasets, function(key, val) {
         for (j = 0; j < val.data.length; ++j) {
            if (val.data[j][0] < pos.x) {
               break;
            }
         }
         var y = val.data[j][1];
         temp[i].innerHTML = key + " = " + addSuffix(y);
         i++;
      });
   };
   */

   function drawGraphs() {
      $.each(datasets, function(key, val) {
         myPlot=$.plot("#"+key, [val.data], {
            legend: { position: "nw", show: true },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax},
            series: { lines: {show: true, fill: true}},
            crosshair: { mode: "x"},
            yaxis: {show: true, min: null, tickFormatter: addSuffix},
            grid: { hoverable: true, autoHighlight: false, clickable: true},
            selection: { mode: "x" }
         });
      });
   }

   drawGraphs();
});
