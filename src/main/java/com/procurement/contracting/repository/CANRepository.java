package com.procurement.contracting.repository;

import com.procurement.contracting.model.entity.CANEntity;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CANRepository extends CassandraRepository<CANEntity, String> {
    @Query(value = "select * from contracting_contract_award_notice where cp_id=?0 and token_entity=?1 limit 1")
    CANEntity getByCpIdAndToken(String cpId, UUID token);

    @Query(value = "select owner from contracting_contract_award_notice where cp_id=?0 and token_entity=?1 limit 1")
    String getOwnerByCpIdAndToken(String cpId, UUID token);

    @Query(value = "select * from contracting_contract_award_notice where cp_id=?0 and award_id=?1 limit 1")
    CANEntity getByCpIdAndAwardId(String cpId, String awardId);
}