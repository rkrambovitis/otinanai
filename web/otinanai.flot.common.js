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

        if (number < 10 )
                result = result.toFixed(2);
        else if (Math.abs(result) < 10 )
                result = result.toFixed(1);
        else if (result.toString().length > 5)
                result = result.toFixed(2);

        return result+suffix;
};
