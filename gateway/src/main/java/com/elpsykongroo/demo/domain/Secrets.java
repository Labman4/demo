
package com.elpsykongroo.demo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.vault.repository.mapping.Secret;

import lombok.Data;

@Secret
@Data
public class Secrets {
    @Id
    public String id;
}
