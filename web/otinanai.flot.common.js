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
   return result.toFixed(1)+suffix;
};


