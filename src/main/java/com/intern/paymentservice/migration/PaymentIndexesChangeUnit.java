package com.intern.paymentservice.migration;

import com.intern.paymentservice.model.Payment;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;


@ChangeUnit(id = "001-add-payment-indexes", order = "001", author = "pon")
public class PaymentIndexesChangeUnit {

    private static final String COLLECTION_NAME = "payments";

    @Execution
    public void changeSet(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Payment.class);

        indexOps.createIndex(new Index()
                .on("orderId", Sort.Direction.ASC)
                .named("payment_order_id_unique_idx"));

        indexOps.createIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .named("payment_user_id_idx"));
        
        indexOps.createIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("payment_status_timestamp_idx"));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Payment.class);
        
        indexOps.dropIndex("payment_order_id_unique_idx");
        indexOps.dropIndex("payment_user_id_idx");
        indexOps.dropIndex("payment_status_timestamp_idx");
    }
}