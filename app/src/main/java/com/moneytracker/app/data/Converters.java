package com.moneytracker.app.data;

import androidx.room.TypeConverter;

import java.util.Date;

/** Room type converters for column types it can't store natively. */
public class Converters {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
