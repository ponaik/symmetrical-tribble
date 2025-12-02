package com.intern.paymentservice.migration;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;

@ChangeUnit(id="client-initializer", order = "001", author = "mongock")
public class ClientInitializerChange {
  private final MongoTemplate mongoTemplate;

    public ClientInitializerChange(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    }
  /** This is the method with the migration code **/
  @Execution
  public void changeSet() {
      //kek

  }
  /**
   This method is mandatory even when transactions are enabled.
   They are used in the undo operation and any other scenario where transactions are not an option.
   However, note that when transactions are avialble and Mongock need to rollback, this method is ignored.
   **/
  @RollbackExecution
  public void rollback() {
    mongoTemplate.delete(new Document());
  }
}