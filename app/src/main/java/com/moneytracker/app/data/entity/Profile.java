package com.moneytracker.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A user profile (e.g. a family member). Each profile owns its own accounts and,
 * transitively through them, its own transactions; categories stay shared/global.
 */
@Entity(tableName = "profiles")
public class Profile {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";

    @NonNull
    @ColumnInfo(name = "pictogram")
    private String pictogram = "pfp_cat";

    @ColumnInfo(name = "sort_order")
    private int sortOrder;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getPictogram() {
        return pictogram;
    }

    public void setPictogram(@NonNull String pictogram) {
        this.pictogram = pictogram;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
