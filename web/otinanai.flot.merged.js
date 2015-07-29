$(function() {
   var i = 0;
   var xmin = null;
   var xmax = null;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
   });

   $("<div class='tooltip'></div>").css({
      position: "absolute",
      display: "none",
      border: "1px solid #fdd",
      padding: "2px",
      "background-color": "#fee",
      opacity: 0.80
   }).appendTo("body");

   function drawGraphs() {
      var it=0;
      var ph=idx;
      var data = [];
      var maxy = null;
      $.each(datasets, function(key, val) {
         if (maxy == null && !stackedGraph)
            maxy = datasets[key]['nn'];
         if (stackedGraph) {
            if (maxy == null)
               maxy = datasets[key]['nn'];
            else
               maxy += datasets[key]['nn'];
         }
         data.push(datasets[key]);
         it++;

         if ( (it % maxMergeCount == 0) || it == Object.keys(datasets).length) {
            $.plot(window.$("#placeholder_"+ph), data, {
               xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max: xmax },
               yaxis: { show: true, tickFormatter: addSuffix, min: null, max: (showSpikes ? null : maxy)},
               grid: { hoverable: true, autoHighlight: false, clickable: true},
               legend: { show: true, position: "nw", sorted: ((stackedGraph) ? "reverse" : "false" )},
	       events: { data: marktext },
               series: { stack: stackedGraph, lines: {show: true, fill: stackedGraph}},
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

            data = [];
            ph++;
            maxy = null;
         }
      });
   };

   drawGraphs();
});
