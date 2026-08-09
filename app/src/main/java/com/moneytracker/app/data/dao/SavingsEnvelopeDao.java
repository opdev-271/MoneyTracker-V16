package com.moneytracker.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.moneytracker.app.data.entity.SavingsEnvelope;

import java.util.List;

@Dao
public interface SavingsEnvelopeDao {

    @Insert
    long insert(SavingsEnvelope envelope);

    @Update
    void update(SavingsEnvelope envelope);

    @Delete
    void delete(SavingsEnvelope envelope);

    @Query("SELECT * FROM savings_envelopes WHERE account_id = :accountId ORDER BY sort_order ASC, id ASC")
    LiveData<List<SavingsEnvelope>> getEnvelopesForAccount(long accountId);

    @Query("SELECT * FROM savings_envelopes WHERE id = :envelopeId")
    SavingsEnvelope getEnvelopeByIdSync(long envelopeId);

    @Query("SELECT IFNULL(SUM(current_amount), 0) FROM savings_envelopes WHERE account_id = :accountId")
    LiveData<Double> getTotalEnvelopedForAccount(long accountId);
}
