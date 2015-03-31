$(function() {
   var i = 0;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
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
      var it=0;
      var ph=0;
      var data = [];
      var maxy = null;
      console.log("maxMergeCount: " + maxMergeCount);
      console.log("datasets.length: " + Object.keys(datasets).length);
      console.log("stacked ? : " + stackedGraph);
      $.each(datasets, function(key, val) {
         if (maxy == null && !stackedGraph)
            maxy = datasets[key]['nn'];
         if (stackedGraph) {
            if (maxy == null)
               maxy = datasets[key]['nn'];
            else
               maxy += datasets[key]['nn'];
         }
         console.log("it: " + it);
         console.log("ph: " + ph);
         console.log("data.length: " + data.length);
         console.log("Pushing: "+key);
         data.push(datasets[key]);
         it++;

         if ( (it % maxMergeCount == 0) || it == Object.keys(datasets).length) {
            console.log("graphing: #placeholder_"+ph);
            $.plot($("#placeholder_"+ph), data, {
               xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: null, max: null },
               yaxis: { show: true, tickFormatter: addSuffix, min: null, max: maxy},
               grid: { hoverable: true, autoHighlight: false, clickable: true},
               legend: {show: true, position: "nw" },
               series: { stack: stackedGraph, lines: {show: true, fill: stackedGraph}}
            //selection: { mode: "xy" }
            });
            $("#placeholder_"+ph).bind("plothover", function (event, pos, item) {
               if (item) {
                  var x = item.datapoint[0], y = item.datapoint[1];

                  $("#tooltip").html(addSuffix(y))
               .css({top: item.pageY+10, left: item.pageX+10})
               .fadeIn(10);
               } else {
                  $("#tooltip").hide();
               }
            });

            data = [];
            ph++;
            maxy = null;
         }
      });
   };

   drawGraphs();
});
