package com.procurement.contracting.dao

import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder.*
import com.procurement.contracting.model.entity.CanEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
class CanDao(private val session: Session) {

    fun save(entity: CanEntity) {
        val insert =
                insertInto(NOTICE_TABLE)
                        .value(CP_ID, entity.cpId)
                        .value(STAGE, entity.stage)
                        .value(CAN_ID, entity.canId)
                        .value(OWNER, entity.owner)
                        .value(CREATED_DATE, entity.createdDate)
                        .value(AWARD_ID, entity.awardId)
                        .value(AC_ID, entity.acId)
                        .value(STATUS, entity.status)
                        .value(STATUS_DETAILS, entity.statusDetails)
        session.execute(insert)
    }

    fun saveAll(entities: List<CanEntity>) {
        val operations = ArrayList<Insert>()
        entities.forEach { entity ->
            operations.add(insertInto(NOTICE_TABLE)
                    .value(CP_ID, entity.cpId)
                    .value(STAGE, entity.stage)
                    .value(CAN_ID, entity.canId)
                    .value(OWNER, entity.owner)
                    .value(CREATED_DATE, entity.createdDate)
                    .value(AWARD_ID, entity.awardId)
                    .value(AC_ID, entity.acId)
                    .value(STATUS, entity.status)
                    .value(STATUS_DETAILS, entity.statusDetails))
        }
        val batch = batch(*operations.toTypedArray())
        session.execute(batch)
    }

    fun findAllByCpIdAndStage(cpId: String, stage: String): List<CanEntity> {
        val query = select()
                .all()
                .from(NOTICE_TABLE)
                .where(eq(CP_ID, cpId))
                .and(eq(STAGE, stage))
        val resultSet = session.execute(query)
        val entities = ArrayList<CanEntity>()
        resultSet.forEach { row ->
            entities.add(
                    CanEntity(
                            cpId = row.getString(CP_ID),
                            stage = row.getString(STAGE),
                            canId = row.getUUID(CAN_ID),
                            owner = row.getString(OWNER),
                            createdDate = row.getTimestamp(CREATED_DATE),
                            awardId = row.getString(AWARD_ID),
                            acId = row.getString(AC_ID),
                            status = row.getString(STATUS),
                            statusDetails = row.getString(STATUS_DETAILS))
            )
        }
        return entities
    }

    companion object {
        private const val NOTICE_TABLE = "contracting_notice"
        private const val CP_ID = "cp_id"
        private const val STAGE = "stage"
        private const val CAN_ID = "can_id"
        private const val OWNER = "owner"
        private const val CREATED_DATE = "created_date"
        private const val AWARD_ID = "award_id"
        private const val AC_ID = "ac_id"
        private const val STATUS = "status"
        private const val STATUS_DETAILS = "status_details"
    }
}