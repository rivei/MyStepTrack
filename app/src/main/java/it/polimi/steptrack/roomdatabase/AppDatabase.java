package it.polimi.steptrack.roomdatabase;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.support.annotation.NonNull;

import it.polimi.steptrack.roomdatabase.dao.DailySummaryDao;
import it.polimi.steptrack.roomdatabase.dao.GPSLocationDao;
import it.polimi.steptrack.roomdatabase.dao.HourlyStepsDao;
import it.polimi.steptrack.roomdatabase.dao.StepDetectedDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingEventDao;
import it.polimi.steptrack.roomdatabase.dao.UserDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingSessionDao;
import it.polimi.steptrack.roomdatabase.entities.DailySummary;
import it.polimi.steptrack.roomdatabase.entities.HourlySteps;
import it.polimi.steptrack.roomdatabase.entities.StepDetected;
import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;
import it.polimi.steptrack.roomdatabase.entities.SensorSample;
import it.polimi.steptrack.roomdatabase.entities.User;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.roomdatabase.entities.GPSLocation;

@Database(entities = {User.class,
        WalkingSession.class,
        GPSLocation.class,
//        GeoFencingEvent.class,
        SensorSample.class,
//        AccelerometerSample.class,
//        GyroscopeSample.class,
        WalkingEvent.class,
        DailySummary.class,
        HourlySteps.class,
        StepDetected.class},
        version = 1,
        exportSchema = false)
@TypeConverters(DateConverter.class)

public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "steptrack_db";
    private static AppDatabase sInstance;

    public abstract UserDao userDao();
    public abstract WalkingSessionDao sessionDao();
    public abstract GPSLocationDao locationDao();
//    public abstract AccelerometerSampleDao accSampleDao();
//    public abstract GyroscopeSampleDao gyroSampleDao();
    public abstract WalkingEventDao walkingEventDao();
//    public abstract GeoFencingEventDao geoFencingEventDao();
    public abstract DailySummaryDao dailySummaryDao();
    public abstract HourlyStepsDao hourlyStepsDao();
    public abstract StepDetectedDao stepDetectedDao();
    private final MutableLiveData<Boolean> mIsDatabaseCreated = new MutableLiveData<>();

//    public static AppDatabase getInstance(final Context context, final AppExecutors executors) {
//        if (sInstance == null) {
//            synchronized (AppDatabase.class) {
//                if (sInstance == null) {
//                    sInstance = buildDatabase(context.getApplicationContext(), executors);
//                    sInstance.updateDatabaseCreated(context.getApplicationContext());
//                }
//            }
//        }
//        return sInstance;
//    }

    public static AppDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
                                    database.setDatabaseCreated();
                                }
                            }).build();
//                            buildDatabase(context.getApplicationContext(), executors);
                    sInstance.updateDatabaseCreated(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }


//    /**
//     * Build the database. {@link Builder#build()} only sets up the database configuration and
//     * creates a new instance of the database.
//     * The SQLite database is only created when it's accessed for the first time.
//     */
//    private static AppDatabase buildDatabase(final Context appContext,
//                                             final AppExecutors executors) {
//        return Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
//                .addCallback(new Callback() {
//                    @Override
//                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
//                        super.onCreate(db);
////                        executors.diskIO().execute(() -> {
////                            // Add a delay to simulate a long-running operation
////                            addDelay();
////                            // Generate the data for pre-population
//                            AppDatabase database = AppDatabase.getInstance(appContext, executors);
////                            List<ProductEntity> products = DataGenerator.generateProducts();
////                            List<CommentEntity> comments =
////                                    DataGenerator.generateCommentsForProducts(products);
////
////                            insertData(database, products, comments);
//                            // notify that the database was created and it's ready to be used
//                            database.setDatabaseCreated();
////                        });
//                    }
//                }).build();
//    }

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

//    private static void insertData(final AppDatabase database, final List<ProductEntity> products,
//                                   final List<CommentEntity> comments) {
//        database.runInTransaction(() -> {
//            //database.productDao().insertAll(products);
//            //database.commentDao().insertAll(comments);
//        });
//    }

//    private static void addDelay() {
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException ignored) {
//        }
//    }

    public LiveData<Boolean> getDatabaseCreated() {
        return mIsDatabaseCreated;
    }
}
