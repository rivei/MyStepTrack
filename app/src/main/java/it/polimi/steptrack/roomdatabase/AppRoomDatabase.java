package it.polimi.steptrack.roomdatabase;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.Executors;

import it.polimi.steptrack.roomdatabase.dao.GPSLocationDao;
import it.polimi.steptrack.roomdatabase.dao.SensorSampleDao;
import it.polimi.steptrack.roomdatabase.dao.UserDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingSessionDao;
import it.polimi.steptrack.roomdatabase.models.SensorSample;
import it.polimi.steptrack.roomdatabase.models.User;
import it.polimi.steptrack.roomdatabase.models.WalkingSession;
import it.polimi.steptrack.roomdatabase.models.GPSLocation;

@Database(entities = {User.class,
        WalkingSession.class,
        GPSLocation.class,
        SensorSample.class},
        version = 1,
        exportSchema = false)
public abstract class AppRoomDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "stepcount_db";
    private static AppRoomDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract WalkingSessionDao sessionDao();
    public abstract GPSLocationDao locationDao();
    public abstract SensorSampleDao sensorSampleDao();
    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

    public static AppRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildDatabase(context);
                    INSTANCE.updateDatabaseCreated(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Build the database. {@link Builder#build()} only sets up the database configuration and
     * creates a new instance of the database.
     * The SQLite database is only created when it's accessed for the first time.
     */
    private static AppRoomDatabase buildDatabase(final Context context){
        return Room.databaseBuilder(context, AppRoomDatabase.class, DATABASE_NAME)
/*TODO: for pre-populate
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executors.newSingleThreadScheduledExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: populate data into database
                                //getInstance(context).dataDao().insertAll(DataEntity.populateData();
                            }
                        });
                    }
                })*/
                .build();
    }

    /**
     * Check whether the database already exists and expose it via {@link #getDatabaseCreated()}
     */
    private void updateDatabaseCreated(final Context context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated();
        }
    }

    private void setDatabaseCreated(){
        mIsDatabaseCreated.postValue(true);
    }

/*    private static void insertData(final AppRoomDatabase database, final List<ProductEntity> products,
                                   final List<CommentEntity> comments) {
        database.runInTransaction(() -> {
            database.productDao().insertAll(products);
            database.commentDao().insertAll(comments);
        });
    }*/

    private static void addDelay() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ignored) {
        }
    }

    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }

    public static void destroyDatabase() {
        INSTANCE = null;
    }

/*    private static RoomDatabase.Callback sRoomDatabaseCallback =
            new RoomDatabase.Callback(){

                @Override
                public void onOpen (@NonNull SupportSQLiteDatabase db){
                    super.onOpen(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };*/


/*    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final UserDao mDao;

        PopulateDbAsync(AppRoomDatabase db) {
            mDao = db.userDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mDao.deleteAll();
            UserDao word = new User;
            mDao.insert(word);
            word = new Word("World");
            mDao.insert(word);
            return null;
        }
    }*/
}
