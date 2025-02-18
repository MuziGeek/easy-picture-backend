package com.muzi.easypicturebackend.esdao;

import com.muzi.easypicturebackend.model.entity.es.EsPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsPostDao extends ElasticsearchRepository<EsPost, Long> {
}
