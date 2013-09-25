$(function() {
   $.each(datasets, function(key, val) {
      $.plot("#"+key, [val.data], {
         legend: { position: "sw" },
         xaxis: { mode: "time", tickDecimals: 0, timezone: "browser"},
         series: { lines: {show: true, fill: true}},
         crosshair: { mode: "x"},
         yaxis: {show: true},
         grid: { hoverable: true, autoHighlight: false}
      });
   });
});
