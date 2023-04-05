package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.Group;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends CrudRepository<Group, String> {
    List<Group> findByAuthorities_Authority(String authority);

    Optional<Group> findByGroupName(String groupName);

    List<Group> findByUsers_Id(String id);

    @Transactional
    int deleteByGroupName(String groupName);

    List<Group> findAll();

}
