package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.Authority;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorityRepository extends CrudRepository<Authority, String> {

    List<Authority> findByGroups_GroupName(String groupName);

    Optional<Authority>  findByAuthority(String authority);

    List<Authority> findByUsers_Id(String id);

    @Transactional
    int deleteByAuthority(String authority);

    List<Authority> findAll();
}
