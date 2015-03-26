$(function() {
   var i = 0;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
   });
/*
   var choiceContainer = $("#choices");

   $.each(datasets, function(key, val) {
      choiceContainer.append("<br/><input type='checkbox' name='" + key + "' checked='checked' id='id" + key + "'></input>" 
         + "<label for='id" + key + "'>" + key + "</label> ");
   });

   choiceContainer.find("input").click(plotAccordingToChoices);
   var updateLegendTimeout = null;
   var latestPosition = null;

   var myPlot = null;
   var myOverview = null;
   updatePlot(datasets);

   var legends = $("#placeholder .legendLabel");

   legends.each(function () {
      $(this).css('width', $(this).width());
   });
	*/

/*
   var xmin = null;
   var xmax = null;
   var ymin = null;
   var ymax = null;
   $("#placeholder").bind("plotselected", function (event, ranges) {
      xmin = ranges.xaxis.from;
      xmax = ranges.xaxis.to;
      ymin = ranges.yaxis.from;
      ymax = ranges.yaxis.to;
      plotAccordingToChoices();
   });

   $("#placeholder").bind("plotunselected", function (event, ranges) {
      xmin = null;
      xmax = null;
      ymin = null;
      ymax = null;
      plotAccordingToChoices();
   });
	$("#placeholder").bind("plothover",  function (event, pos, item) {
		latestPosition = pos;
		if (!updateLegendTimeout) {
			updateLegendTimeout = setTimeout(updateLegend, 50);
		}
		updateLegendTimeout = false;
	});
	*/




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
		console.log("maxMergeCount: " + maxMergeCount);
		console.log("datasets.length: " + Object.keys(datasets).length);
		console.log("stacked ? : " + stackedGraph);
      $.each(datasets, function(key, val) {
			console.log("it: " + it);
			console.log("ph: " + ph);
			console.log("data.length: " + data.length);
			if ( it < maxMergeCount ) {
				console.log("Pushing: "+key);
				data.push(datasets[key]);
				it++;
			}

			if ( (it % maxMergeCount == 0) || it == Object.keys(datasets).length) {
				console.log("graphing: #placeholder_"+ph);
				$.plot($("#placeholder_"+ph), data, {
					xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: null, max: null },
					yaxis: { show: true, tickFormatter: addSuffix, min: null, max: null},
					grid: { hoverable: true, autoHighlight: false, clickable: true},
					legend: {show: true, position: "nw" },
					series: { stack: stackedGraph, lines: {show: true, fill: stackedGraph}},
					selection: { mode: "xy" }
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
			}
      });
   };

   drawGraphs();
/*
	function updateLegend() {
		var temp = $('#placeholder .legendLabel');
		var pos = latestPosition;
		var axes = myPlot.getAxes();
		if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
			return;
		}
		var j,i = 0;
		$.each(datasets, function(key, val) {
			if (val.data.length <= 2) {
			} else {
				for (j = 0; j < val.data.length; ++j) {
					if (val.data[j][0] > pos.x) {
						break;
					}
				}
				var y = val.data[j][1];
				temp[i].innerHTML = key + " = " + addSuffix(y);
			}
			i++;
		});
	};

   function updatePlot(data) {
      if (data.length > 0) {
         currentData=data;
         myPlot=$.plot("#placeholder", data, {
            legend: { position: "nw", show: "true" },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max:xmax},
            yaxis: {show: true, min: ymin, max: ymax},
            series: { lines: {show: true, fill: false}},
            crosshair: { mode: "y" },
            grid: { hoverable: true, autoHighlight: false},
            selection: { mode: "xy" }
         });
		}
   }

   function plotAccordingToChoices() {
      var data = [];
      var foo = 0;
      choiceContainer.find("input:checked").each(function () {
         var key = $(this).attr("name");
			data.push(datasets[key]);
		});
      updatePlot(data);
		updateLegend();
   }

   plotAccordingToChoices();
*/
});
