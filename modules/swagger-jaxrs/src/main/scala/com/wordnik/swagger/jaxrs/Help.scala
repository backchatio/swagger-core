/**
 *  Copyright 2011 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wordnik.swagger.jaxrs

import com.wordnik.swagger.core._

import org.slf4j.LoggerFactory

import com.sun.jersey.api.core.ResourceConfig

import javax.servlet.ServletConfig

import javax.ws.rs.{ Path, GET }
import javax.ws.rs.core.{ UriInfo, HttpHeaders, Context, Response }
import javax.ws.rs.core.Response.Status

import scala.collection.JavaConversions._

trait Help {
  @GET
  @ApiOperation(value = "Returns information about API parameters",
    responseClass = "com.wordnik.swagger.core.Documentation")
  def getHelp(@Context sc: ServletConfig,
    @Context rc: ResourceConfig,
    @Context headers: HttpHeaders,
    @Context uriInfo: UriInfo): Response = {
    val reader = ConfigReaderFactory.getConfigReader(sc)

    val apiVersion = reader.getApiVersion()
    val swaggerVersion = reader.getSwaggerVersion()
    val basePath = reader.getBasePath()
    val apiFilterClassName = reader.getApiFilterClassName()

    val filterOutTopLevelApi = true
    val currentApiEndPoint = this.getClass.getAnnotation(classOf[Api])
    if (currentApiEndPoint == null) {
      //  TODO: handle this
      Response.status(Status.NOT_FOUND).build
    } else {
      val apiPath = {
        if (filterOutTopLevelApi) {
          currentApiEndPoint.value
        } else null
      }
      val apiListingPath = {
        if (filterOutTopLevelApi) {
          if (!"".equals(currentApiEndPoint.listingPath)) currentApiEndPoint.listingPath
          else currentApiEndPoint.value
        } else null
      }
      val listingClass: Class[_] = {
        if (currentApiEndPoint.listingClass != "") {
          SwaggerContext.loadClass(currentApiEndPoint.listingClass)
        } else this.getClass
      }
      val docs = new HelpApi(apiFilterClassName).filterDocs(
        ApiReader.read(listingClass, apiVersion, swaggerVersion, basePath, apiPath),
        headers,
        uriInfo,
        apiListingPath)
      Response.ok.entity(docs).build
    }
  }
}

object ConfigReaderFactory {
  def getConfigReader(sc: ServletConfig): ConfigReader = {
    var configReaderStr = {
      if (sc.getInitParameter("swagger.config.reader") == null) "com.wordnik.swagger.core.ConfigReader"
      else sc.getInitParameter("swagger.config.reader")
    }
    val constructor = SwaggerContext.loadClass(configReaderStr).getConstructor(classOf[ServletConfig])
    val configReader = constructor.newInstance(sc).asInstanceOf[ConfigReader]
    configReader
  }
}