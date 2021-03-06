/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.griffin.measure.context.datasource.connector.batch

import org.apache.griffin.measure.configuration.params.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.context.datasource.info.TmstCache
import org.apache.griffin.measure.utils.HdfsUtil
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.griffin.measure.utils.ParamUtil._

/**
  * batch data connector for avro file
  */
case class AvroBatchDataConnector(@transient sparkSession: SparkSession,
                                  dcParam: DataConnectorParam,
                                  tmstCache: TmstCache
                                 ) extends BatchDataConnector {

  val config = dcParam.getConfig

  val FilePath = "file.path"
  val FileName = "file.name"

  val filePath = config.getString(FilePath, "")
  val fileName = config.getString(FileName, "")

  val concreteFileFullPath = if (pathPrefix) s"${filePath}${fileName}" else fileName

  private def pathPrefix(): Boolean = {
    filePath.nonEmpty
  }

  private def fileExist(): Boolean = {
    HdfsUtil.existPath(concreteFileFullPath)
  }

  def data(ms: Long): (Option[DataFrame], TimeRange) = {
    val dfOpt = try {
      val df = sparkSession.read.format("com.databricks.spark.avro").load(concreteFileFullPath)
      val dfOpt = Some(df)
      val preDfOpt = preProcess(dfOpt, ms)
      preDfOpt
    } catch {
      case e: Throwable => {
        error(s"load avro file ${concreteFileFullPath} fails")
        None
      }
    }
    val tmsts = readTmst(ms)
    (dfOpt, TimeRange(ms, tmsts))
  }


}
