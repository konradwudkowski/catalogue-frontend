/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cataloguefrontend

import com.github.tototoshi.csv.CSVWriter
import org.apache.commons.io.output.ByteArrayOutputStream


object Chicken {

  import purecsv.unsafe._
  case class Event(ts: Long, msg: String)
  val records = CSVReader[Event].readCSVFromString("1,foo\n2,bar")

  import scala.reflect.runtime.universe._

  def classAccessors[T: TypeTag]: List[MethodSymbol] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  def main(args: Array[String]): Unit = {
    val headers = classAccessors[Event].map(_.name)
    println(headers.mkString(","))
  //    println(records.toCSV())


//    writer.writeAll(Seq(Seq("name", "surname", "country"), Seq("armin", "keyvanloo", "UK")))
//    records.zip()
//    val allVals = records.map(event => event.productElement().toSeq)
//
//    writeCsv(allVals)
  }

  private def writeCsv(allVals: List[Seq[Any]]) = {
    val os = new ByteArrayOutputStream()
    val writer = CSVWriter.open(new java.io.OutputStreamWriter(os))
    writer.writeAll(allVals)
    println(os.toString)
    os.close()
  }
}
