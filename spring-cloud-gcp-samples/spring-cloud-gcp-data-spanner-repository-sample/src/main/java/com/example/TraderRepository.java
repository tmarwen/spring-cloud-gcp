/*
 * Copyright 2017-2018 the original author or authors.
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

package com.example;

import java.util.List;

import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import com.google.cloud.spring.data.spanner.repository.query.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * A sample repository.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
@RepositoryRestResource(collectionResourceRel = "traders_repository", path = "traders")
public interface TraderRepository extends SpannerRepository<Trader, String> {
	@Query("SELECT count(1) from traders_repository where JSON_VALUE(work_address, '$.active') = @active")
	long getCountActive(@Param("active") String active);


	@Query("SELECT work_address from traders_repository where JSON_VALUE(work_address, '$.active') = @active")
	List<Address> getTraderWorkAddressByActive(@Param("active") String active);
}
