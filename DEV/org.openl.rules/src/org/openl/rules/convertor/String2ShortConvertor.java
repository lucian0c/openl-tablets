package org.openl.rules.convertor;

public class String2ShortConvertor extends String2NumberConverter<Short> {

    @Override
    Short convert(Number number, String data) {
        double dValue = number.doubleValue();
        if (dValue > Short.MAX_VALUE || dValue < Short.MIN_VALUE) {
            throw new NumberFormatException("A number is out of range [-32768...+32767]");
        }
        return number.shortValue();
    }
}
