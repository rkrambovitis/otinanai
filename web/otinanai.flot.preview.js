$(function() {

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
   });


   function drawGraphs() {
      $.each(datasets, function(key, val) {
         $.plot("#"+key, [val.data], {
            legend: { position: "sw", show: true },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax},
            series: { lines: {show: true, fill: true}},
            crosshair: { mode: "x"},
            yaxis: {show: true},
            grid: { hoverable: true, autoHighlight: false, clickable: true},
            selection: { mode: "x" }
         });
      });
   }

   drawGraphs();
});
