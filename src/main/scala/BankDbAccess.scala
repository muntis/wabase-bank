package uniso.app

import org.tresql.ThreadLocalResources
import org.wabase._


object BankQuereaseIo extends AppQuereaseIo[org.wabase.Dto](BankQuerease) {

}

trait BankDbAccess extends DbAccess { this: Loggable =>


  override implicit lazy val tresqlResources = new BankTresqlResources

  class BankTresqlResources extends ThreadLocalResources {
    override def initResourcesTemplate = {
      BankDbAccess.this.resourcesTemplate
    }
  }
}


trait BankQuerease extends AppQuerease with BankMetadata {



}

object BankQuerease extends BankQuerease
