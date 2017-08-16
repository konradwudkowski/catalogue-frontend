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


object Chicken2 extends App {


  def ccToMapOld(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
      (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc))
    }

  def ccToMap(cc: AnyRef) =
    cc.getClass.getDeclaredFields.foldLeft(Map[String, Any]()) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }

  // Usage

  case class Column(name: String,
                    typeCode: Int,
                    typeName: Option[String],
                    size: Int = 0)

  val column = Column("id", 0, Some("INT"), 11)
  println(ccToMap(column))
}
