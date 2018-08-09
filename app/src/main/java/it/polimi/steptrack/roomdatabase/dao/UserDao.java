package it.polimi.steptrack.roomdatabase.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.models.User;
import it.polimi.steptrack.roomdatabase.models.User;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface UserDao {
    @Query("select * from users")
    List<User> loadAllUsers();

    @Query("select * from users where uid = :id")
    User loadUserById(int id);

    @Query("select * from users where user_name = :firstName") //TODO
    List<User> findUserByNameAndLastName(String firstName);

    @Insert(onConflict = IGNORE)
    void insertUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("delete from users where user_name like :badName")
    int deleteUsersByName(String badName);

    @Insert(onConflict = IGNORE)
    void insertOrReplaceUsers(User... users);

    @Delete
    void deleteUsers(User user1, User user2);

    @Query("DELETE FROM users")
    void deleteAll();
}
