$(function() {
   var i = 0;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
   });

   var choiceContainer = $("#choices");

   $.each(datasets, function(key, val) {
      choiceContainer.append("<br/><input type='checkbox' name='" + key + "' checked='checked' id='id" + key + "'></input>" 
         + "<label for='id" + key + "'>" + key + "</label> "
         + "<a href="+key+">(alone)</a>");
   });

   choiceContainer.find("input").click(plotAccordingToChoices);
   var updateLegendTimeout = null;
   var latestPosition = null;

   var myplot = null;
   updatePlot(datasets);

   var legends = $("#placeholder .legendLabel");

   legends.each(function () {
      $(this).css('width', $(this).width());
   });

   $("#placeholder").bind("plothover",  function (event, pos, item) {
      latestPosition = pos;
      if (!updateLegendTimeout) {
         updateLegendTimeout = setTimeout(updateLegend, 50);
      }
   });

   function updateLegend() {
      var temp = $('#placeholder .legendLabel');
      updateLegendTimeout = null;
      var pos = latestPosition;
      var axes = myplot.getAxes();
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
         //temp[i].innerHTML = series.label + " = " + addSuffix(y);
         temp[i].innerHTML = key + " = " + addSuffix(y);
         i++;
      });
   };





   function updatePlot(data) {
      if (data.length > 0) {
         currentData=data;
         myplot=$.plot("#placeholder", data, {
            legend: { position: "sw" },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser"},
            series: { lines: {show: true, fill: true}},
            crosshair: { mode: "x"},
            yaxis: {show: false},
            grid: { hoverable: true, autoHighlight: false}
         });
      }
   }

   function plotAccordingToChoices() {
      var data = [];
      choiceContainer.find("input:checked").each(function () {
         var key = $(this).attr("name");
         if (key && datasets[key]) {
            data.push(datasets[key]);
         }
      });
      updatePlot(data);
      updateLegend();
   }

   plotAccordingToChoices();

   $("#placeholder").bind("plotselected", function (event, ranges) {
      if (ranges.xaxis.to - ranges.xaxis.from < 0.00001) {
         ranges.xaxis.to = ranges.xaxis.from + 0.00001;
      }

      if (ranges.yaxis.to - ranges.yaxis.from < 0.00001) {
         ranges.yaxis.to = ranges.yaxis.from + 0.00001;
      }

      plot = $.plot("#placeholder", getData(ranges.xaxis.from, ranges.xaxis.to),
         $.extend(true, {}, options, {
            xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to },
         yaxis: { min: ranges.yaxis.from, max: ranges.yaxis.to }
         })
      );
   });
});
