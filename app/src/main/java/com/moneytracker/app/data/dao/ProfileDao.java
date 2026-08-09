package com.moneytracker.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.moneytracker.app.data.entity.Profile;

import java.util.List;

@Dao
public interface ProfileDao {

    @Insert
    long insert(Profile profile);

    @Update
    void update(Profile profile);

    @Delete
    void delete(Profile profile);

    @Query("SELECT * FROM profiles ORDER BY sort_order ASC, id ASC")
    LiveData<List<Profile>> getAllProfiles();

    @Query("SELECT * FROM profiles ORDER BY sort_order ASC, id ASC")
    List<Profile> getAllProfilesSync();

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    LiveData<Profile> getProfileById(long profileId);

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    Profile getProfileByIdSync(long profileId);

    @Query("SELECT COUNT(*) FROM profiles")
    int countSync();
}
