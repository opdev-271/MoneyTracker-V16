package com.moneytracker.app.data.repository;

import androidx.lifecycle.LiveData;

import com.moneytracker.app.data.AppDatabase;
import com.moneytracker.app.data.dao.ProfileDao;
import com.moneytracker.app.data.entity.Profile;
import com.moneytracker.app.util.AppExecutors;

import java.util.List;

public class ProfileRepository {

    private final ProfileDao profileDao;
    private final AppExecutors executors;

    public ProfileRepository(AppDatabase database, AppExecutors executors) {
        this.profileDao = database.profileDao();
        this.executors = executors;
    }

    public LiveData<List<Profile>> getAllProfiles() {
        return profileDao.getAllProfiles();
    }

    public LiveData<Profile> getProfileById(long profileId) {
        return profileDao.getProfileById(profileId);
    }

    public void insert(Profile profile, OnProfileSaved callback) {
        executors.diskIO().execute(() -> {
            long id = profileDao.insert(profile);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onSaved(id));
            }
        });
    }

    public void update(Profile profile) {
        executors.diskIO().execute(() -> profileDao.update(profile));
    }

    public void delete(Profile profile) {
        executors.diskIO().execute(() -> profileDao.delete(profile));
    }

    public interface OnProfileSaved {
        void onSaved(long profileId);
    }
}
