/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/message/Send.java
package com.elpsykongroo.base.domain.message;
=======
package com.elpsykongroo.demo.repo.elasticsearch;
>>>>>>> main:base/src/main/java/com/elpsykongroo/base/domain/message/elasticsearch/AccessRecordRepo.java

import lombok.Data;

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/message/Send.java
@Data
public class Send {
    private String id;

    private String groupId;

    private String topic;

    private String callback;

    private String key;
=======
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.elpsykongroo.demo.domain.AccessRecord;

public interface AccessRecordRepo extends ElasticsearchRepository<AccessRecord, String> {
	Page<AccessRecord> findAll(Pageable pageable);
>>>>>>> main:base/src/main/java/com/elpsykongroo/base/domain/message/elasticsearch/AccessRecordRepo.java

    private String message;

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/message/Send.java
    private byte[] data;

    private String sha256;
=======
	List<AccessRecord> findByAccessPath(String path);

	List<AccessRecord> findByUserAgentLike(String agent);

	List<AccessRecord> findByRequestHeaderLike(String header);

>>>>>>> main:base/src/main/java/com/elpsykongroo/base/domain/message/elasticsearch/AccessRecordRepo.java

    private boolean manualStop;

    private String offset;
}
