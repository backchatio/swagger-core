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
package com.wordnik.swagger.core

import org.slf4j.LoggerFactory

import com.sun.jersey.api.core.ResourceConfig

import javax.ws.rs.{ Path, GET }
import javax.ws.rs.core.{ UriInfo, HttpHeaders, Context, Response }
import javax.servlet.ServletConfig

import scala.collection.JavaConversions._

trait ApiListing {
  private val logger = LoggerFactory.getLogger(classOf[ApiListing])

  @GET
  @ApiOperation(value = "Returns list of all available api endpoints",
    responseClass = "DocumentationEndPoint", multiValueResponse = true)
  def getAllApis(@Context sc: ServletConfig,
    @Context rc: ResourceConfig,
    @Context headers: HttpHeaders,
    @Context uriInfo: UriInfo): Response = {
    
    val configReader = ConfigReaderFactory.getConfigReader(sc)
    val apiVersion = configReader.getApiVersion()
    val swaggerVersion = configReader.getSwaggerVersion()
    val basePath = configReader.getBasePath()
    val apiFilterClassName = configReader.getApiFilterClassName()
    var apiFilter: AuthorizationFilter = null
    if (null != apiFilterClassName) {
      try {
        logger.debug("loading filter class" + apiFilterClassName)
        apiFilter = SwaggerContext.loadClass(apiFilterClassName).newInstance.asInstanceOf[AuthorizationFilter]
      } catch {
        case e: ClassNotFoundException => logger.error("Unable to resolve apiFilter class " + apiFilterClassName);
        case e: ClassCastException => logger.error("Unable to cast to apiFilter class " + apiFilterClassName);
      }
    }

    val resources = rc.getRootResourceClasses
    val apiListingEndpoint = this.getClass.getAnnotation(classOf[Api])
    val resourceListingType = this.getClass.getAnnotation(classOf[javax.ws.rs.Produces]).value.toSet
    println(resourceListingType)
    
    val allApiDoc = new Documentation
    resources.foreach(resource => {
      val wsPath = resource.getAnnotation(classOf[Api])
      logger.debug("processing resource path " + wsPath)
      if (null != wsPath && wsPath.value != ApiReader.LIST_RESOURCES_PATH) {
        val path = {
          if ("" != wsPath.listingPath) wsPath.listingPath
          else wsPath.value + ApiReader.FORMAT_STRING
        }

        logger.debug("adding api " + path + ", " + wsPath.description())

        val shouldAddDocumentation = {
          if (wsPath.listingPath != "" && wsPath.listingClass == "") {
            logger.debug("skipping documentation for " + wsPath.listingPath + ", " + wsPath.value)
            false
          }
          else {
            logger.debug("adding documentation for " + wsPath.listingPath + ", " + wsPath.listingClass)
            true
          }
        }
        
        val hasCompatibleMediaType = {
          // check accept type first
          val resourceMediaType = {
            if(headers.getRequestHeaders().contains("Accept")) {
              logger.debug("using accept headers")
              headers.getRequestHeaders()("Accept").toSet
            }
            else {
              logger.debug("using produces annotations")
              resource.getAnnotation(classOf[javax.ws.rs.Produces]).value.toSet
            }
          }
          println(resourceMediaType)
          
          // nothing found, check produces type
          var hasMatch = false
          resourceMediaType.foreach(rt => {
            if(resourceListingType.contains(rt)) {
              logger.debug("matched " + rt)
              hasMatch = true
            }
          })
          hasMatch
        }
        
        if(!hasCompatibleMediaType) logger.debug("no compatible media types")
        
        if (shouldAddDocumentation && hasCompatibleMediaType) {
          var api = new DocumentationEndPoint(path, wsPath.description())
          if (!isApiAdded(allApiDoc, api)) {
            if (null != apiFilter) {
              apiFilter match {
                case apiAuthFilter: ApiAuthorizationFilter => {
                  if (apiAuthFilter.authorizeResource(api.path, headers, uriInfo))
                    allApiDoc.addApi(api)
                }
                case fineGrainedApiAuthFilter: FineGrainedApiAuthorizationFilter => {
                  if (fineGrainedApiAuthFilter.authorizeResource(api.path, api, headers, uriInfo))
                    allApiDoc.addApi(api)
                }
                case _ =>
              }
            } else {
              allApiDoc.addApi(api)
            }
          }
        }
      } else logger.debug("no data for path " + wsPath)
    })

    allApiDoc.swaggerVersion = swaggerVersion
    allApiDoc.basePath = basePath
    allApiDoc.apiVersion = apiVersion

    Response.ok.entity(allApiDoc).build
  }

  private def isApiAdded(allApiDoc: Documentation, endpoint: DocumentationEndPoint): Boolean = {
    var isAdded: Boolean = false
    if (allApiDoc.getApis != null) {
      for (addedApi <- allApiDoc.getApis()) {
        if (endpoint.path.equals(addedApi.path)) isAdded = true
      }
    }
    isAdded
  }
}
