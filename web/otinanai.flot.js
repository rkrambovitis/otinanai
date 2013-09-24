$(function() {
   var i = 0;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
   });

   var choiceContainer = $("#choices");

   $.each(datasets, function(key, val) {
      choiceContainer.append("<br/><input type='checkbox' name='" + key + "' checked='checked' id='id" + key + "'></input>" 
         + "<label for='id" + key + "'>" + val.label + "</label> "
         + "<a href="+key+">(alone)</a>");
   });

   choiceContainer.find("input").click(plotAccordingToChoices);

   var legends = $("#placeholder .legendLabel");

   legends.each(function () {
      $(this).css('width', $(this).width());
   });

   var updateLegendTimeout = null;
   var latestPosition = null;

   var myplot = null;
   updatePlot(datasets);

   function updateLegend() {
      var temp = $('#placeholder .legendLabel');
      updateLegendTimeout = null;
      var pos = latestPosition;
      var axes = myplot.getAxes();
      if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
         return;
      }
      var i, j, dataset = myplot.getData();
      for (i = 0; i < dataset.length; ++i) {
         var series = dataset[i];
         for (j = 0; j < series.data.length; ++j) {
            if (series.data[j][0] < pos.x) {
               break;
            }
         }

         /*
         var y,p1 = series.data[j - 1],p2 = series.data[j];
         if (p1 == null) {
            y = p2[1];
         }else if (p2 == null) {
            y = p1[1];
         } else {
            y = p1[1] + (p2[1] - p1[1]) * (pos.x - p1[0]) / (p2[0] - p1[0]);
         
         }
         temp[i].innerHTML = series.label + " = " + addSuffix(y);//.toFixed(2);
         */
         var y = series.data[j][1];
         temp[i].innerHTML = series.label + " = " + addSuffix(y);
      }
   };

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
      return result.toFixed(2)+suffix;
   };

   $("#placeholder").bind("plothover",  function (event, pos, item) {
      latestPosition = pos;
      if (!updateLegendTimeout) {
         updateLegendTimeout = setTimeout(updateLegend, 50);
      }
   });

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
