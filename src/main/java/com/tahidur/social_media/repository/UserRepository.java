package com.tahidur.social_media.repository;

import com.tahidur.social_media.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tahidur.social_media.model.Users;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);

    @Query("SELECT new com.tahidur.social_media.model.Users(u.username, u.isOnline) FROM AppUser u")
    List<Users> userStatues();
}
