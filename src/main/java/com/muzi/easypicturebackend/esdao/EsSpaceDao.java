package com.muzi.easypicturebackend.esdao;

import com.muzi.easypicturebackend.model.entity.es.EsSpace;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSpaceDao extends ElasticsearchRepository<EsSpace, Long> {
}
