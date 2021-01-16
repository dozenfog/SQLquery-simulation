package com.convertation;

public enum RequestName {

    SHORT_NAME (1),
    TYPE_OF_WORK (2),
    EMPLOYEES (3),
    DEFAULT(4);

    public int number;

    RequestName(int number) {
        this.number = number;
    }

    public static RequestName castFromStringToEnum(String value) {
        for (RequestName name : values()) {
            if (name.toString().equalsIgnoreCase(value)) {
                return name;
            }
        }
        return RequestName.DEFAULT;
    }
}
