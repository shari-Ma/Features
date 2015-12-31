package com.malata.superclean.views.textcounter.formatters;


import com.malata.superclean.views.textcounter.Formatter;

import java.text.DecimalFormat;

/**
 * Created by prem on 10/28/14.
 */
public class DecimalFormatter implements Formatter {

    private final DecimalFormat format = new DecimalFormat("#.0");

    @Override
    public String format(String prefix, String suffix, float value) {
        return prefix + format.format(value) + suffix;
    }
}
