package uniso.app

import org.wabase.DataBaseSpecs

class DataSpecs extends DataBaseSpecs with WithHttpClient{
  override def initHttpClient: org.wabase.client.WabaseHttpClient = super[WithHttpClient].initHttpClient

}
