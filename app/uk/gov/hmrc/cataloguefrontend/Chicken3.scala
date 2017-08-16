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
import com.opencsv.bean.StatefulBeanToCsvBuilder
import org.apache.commons.io.output.ByteArrayOutputStream

import scala.collection.JavaConverters.asJavaIterableConverter

import collection.JavaConverters._

object Chicken3 extends App {

  import java.io.FileWriter
  import java.io.Writer
  // List<MyBean> beans comes from somewhere earlier in your code.// List<MyBean> beans comes from somewhere earlier in your code.

  class Foo(bar:String, baz:Int)

  val writer = new FileWriter("yourfile.csv")
  val beanToCsv = new StatefulBeanToCsvBuilder(writer).build
//  beanToCsv.write(new Foo("a", 1))

}
