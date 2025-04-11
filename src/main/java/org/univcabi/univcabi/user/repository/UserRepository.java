package org.univcabi.univcabi.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.univcabi.univcabi.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {


}
