package com.convertation;

public enum CommandName {
    FROM(1),
    WHERE(2),
    INTO(3),
    DEFAULT(4);

    public int value;

    CommandName(int value) {
        this.value = value;
    }

    public static CommandName castFromStringToEnum(String value) {
        for (CommandName name : values()) {
            if (name.toString().equalsIgnoreCase(value)) {
                return name;
            }
        }
        return CommandName.DEFAULT;
    }
}
